package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


@Slf4j
@Component
public class FactorySchedulingConstraintProvider implements ConstraintProvider, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // 硬约束
                workCenterConstraint(constraintFactory),
//                machineCapacityConstraint(constraintFactory),
                procedureSequenceConstraint(constraintFactory),
//                orderDateConstraint(constraintFactory),
//                // 软约束
//                minimizeMakespanConstraint(constraintFactory),
//                encourageEarlyStartConstraint(constraintFactory),
//                orderPriorityConstraint(constraintFactory),
//                balanceMachineLoadConstraint(constraintFactory),
//                minimizeProcedureGapConstraint(constraintFactory)
        };
    }

    /**
     * 硬约束1: 工作中心约束
     * 工作中心和工作中心日历必须匹配
     * 工作中心状态为N则表示该机器当前不可用
     */
    public Constraint workCenterConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> !Objects.isNull(timeslot.getMaintenance()))
                .filter(timeslot -> {
                    WorkCenter workCenter = timeslot.getWorkCenter();
                    WorkCenterMaintenance maintenance = timeslot.getMaintenance();
                    // 检查工作中心状态是否为N（不可用）
                    if ("n".equals(workCenter.getStatus())) {
                        return true; // 违反约束：机器不可用
                    }
                    // 检查工作中心和工作中心日历是否匹配
                    return !workCenter.getWorkCenterCode().equals(maintenance.getWorkCenter().getWorkCenterCode()); // 违反约束：工作中心不匹配
                }).penalize(HardSoftScore.ONE_HARD).asConstraint("工作中心约束");
    }

    /**
     * 硬约束2: 机器容量约束
     * 确保每台机器每天的总工作时间不超过其容量,尽量每天留出60分钟剩余容量
     */
    public Constraint machineCapacityConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null && timeslot.getMaintenance() != null)
                .groupBy(
                        timeslot -> timeslot.getMaintenance(),
                        ConstraintCollectors.sum(timeslot -> timeslot.getDuration())
                )
                .filter((maintenance, totalDuration) -> totalDuration > maintenance.getRemainingCapacity())
                .penalize(HardSoftScore.ONE_HARD, (maintenance, totalDuration) -> {
                    // 计算超过容量的部分
                    int excess = totalDuration - maintenance.getRemainingCapacity();
                    return Math.max(0, excess);
                })
                .asConstraint("机器容量约束");
    }

    /**
     * 硬约束3: 工序顺序约束
     * 当前工序完成时间必须早于下一道工序开始时间，处理并行工序逻辑
     */
    public Constraint procedureSequenceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .join(Timeslot.class,
                        Joiners.equal(timeslot -> timeslot.getProcedure().getTaskNo(), timeslot -> timeslot.getProcedure().getTaskNo()),
                        Joiners.filtering((timeslot, timeslot2) ->
                                timeslot.getProcedure().getNextProcedure().contains(timeslot2.getProcedure())
                                        && timeslot.getStartTime() != null && timeslot2.getEndTime() != null)
                )
                .filter((timeslot1, timeslot2) -> {
                    // 检查时间顺序：timeslot1的结束时间必须早于timeslot2的开始时间
                    if (timeslot1.getEndTime() == null || timeslot2.getStartTime() == null) {
                        return false;
                    }
                    return timeslot1.getEndTime().isAfter(timeslot2.getStartTime());
                })
                .penalize(HardSoftScore.ONE_HARD, (timeslot1, timeslot2) -> {
                    // 计算违反顺序的时间重叠分钟数
                    if (timeslot1.getEndTime() == null || timeslot2.getStartTime() == null) {
                        return 0;
                    }

                    long overlapMinutes = Duration.between(timeslot2.getStartTime(), timeslot1.getEndTime()).toMinutes();
                    return (int) Math.max(0, overlapMinutes);
                })
                .asConstraint("工序顺序约束");
    }

    /**
     * 硬约束4: 订单日期约束
     * 尊重实际开始时间和结束时间，当factStartDate不为空时必须按照实际时间安排
     */
    public Constraint orderDateConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getOrder() != null)
                .filter(timeslot -> {
                    Order order = timeslot.getOrder();
                    LocalDateTime factStartDate = order.getFactStartDate();
                    LocalDateTime factEndDate = order.getFactEndDate();

                    // 如果实际开始时间不为空，必须遵守
                    if (factStartDate != null) {
                        if (timeslot.getStartTime() == null) {
                            return true; // 没有开始时间，违反约束
                        }
                        // 开始时间不能早于实际开始时间
                        if (timeslot.getStartTime().isBefore(factStartDate)) {
                            return true;
                        }
                    }

                    // 如果实际结束时间不为空，必须遵守
                    if (factEndDate != null) {
                        if (timeslot.getEndTime() == null) {
                            return true; // 没有结束时间，违反约束
                        }
                        // 结束时间不能晚于实际结束时间
                        if (timeslot.getEndTime().isAfter(factEndDate)) {
                            return true;
                        }
                    }

                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD, timeslot -> {
                    Order order = timeslot.getOrder();
                    LocalDateTime factStartDate = order.getFactStartDate();
                    LocalDateTime factEndDate = order.getFactEndDate();
                    int penalty = 0;

                    // 计算违反实际开始时间的惩罚
                    if (factStartDate != null && timeslot.getStartTime() != null && timeslot.getStartTime().isBefore(factStartDate)) {
                        long minutesEarly = Duration.between(timeslot.getStartTime(), factStartDate).toMinutes();
                        penalty += (int) minutesEarly;
                    }

                    // 计算违反实际结束时间的惩罚
                    if (factEndDate != null && timeslot.getEndTime() != null && timeslot.getEndTime().isAfter(factEndDate)) {
                        long minutesLate = Duration.between(factEndDate, timeslot.getEndTime()).toMinutes();
                        penalty += (int) minutesLate;
                    }

                    // 如果没有时间，给一个大惩罚
                    if (timeslot.getStartTime() == null || timeslot.getEndTime() == null) {
                        penalty += 1000;
                    }

                    return penalty;
                })
                .asConstraint("订单日期约束");
    }

    /**
     * 软约束1: 最小化总完成时间(Makespan)
     * 最小化所有订单的最晚完成时间
     */
    public Constraint minimizeMakespanConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getEndTime() != null)
                .groupBy(
                        ConstraintCollectors.max((Timeslot timeslot) -> timeslot.getEndTime().toEpochSecond(java.time.ZoneOffset.UTC))
                )
                .penalize(HardSoftScore.ONE_SOFT, (Long maxEndTimeEpoch) -> {
                    // 将最晚结束时间转换为分钟作为惩罚值
                    // 越晚的结束时间惩罚越大
                    return (int) (maxEndTimeEpoch / 60); // 转换为分钟
                })
                .asConstraint("最小化总完成时间");
    }

    /**
     * 软约束2: 鼓励提前开始
     * 订单越早开始越好，特别是高优先级订单
     */
    public Constraint encourageEarlyStartConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null && timeslot.getTask() != null)
                .penalize(HardSoftScore.ONE_SOFT, timeslot -> {
                    Task task = timeslot.getTask();
                    int priority = task.getPriority();

                    // 将开始时间转换为从某个基准时间开始的分钟数
                    // 越早开始惩罚越小，但高优先级订单更鼓励早开始
                    LocalDateTime startTime = timeslot.getStartTime();
                    LocalDateTime baseTime = LocalDateTime.of(2024, 1, 1, 0, 0); // 基准时间
                    long minutesFromBase = Duration.between(baseTime, startTime).toMinutes();

                    // 基础惩罚：开始时间越晚惩罚越大
                    int basePenalty = (int) minutesFromBase;

                    // 优先级调整：高优先级订单更鼓励早开始
                    // 优先级越高（数值越大），对晚开始的惩罚越大
                    int priorityMultiplier = Math.max(1, priority);

                    return basePenalty * priorityMultiplier;
                })
                .asConstraint("鼓励提前开始");
    }

    /**
     * 软约束3: 订单优先级约束
     * 高优先级订单应该在低优先级订单之前完成
     */
    public Constraint orderPriorityConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getOrder() != null && timeslot.getTask() != null && timeslot.getEndTime() != null)
                .join(Timeslot.class,
                        Joiners.filtering((timeslot1, timeslot2) -> {
                            // 检查两个时间槽是否属于不同的订单
                            Order order1 = timeslot1.getOrder();
                            Order order2 = timeslot2.getOrder();

                            if (order1 == null || order2 == null) {
                                return false;
                            }

                            // 确保是不同的订单
                            if (order1.getOrderNo().equals(order2.getOrderNo())) {
                                return false;
                            }

                            // 获取对应的Task以检查优先级
                            Task task1 = timeslot1.getTask();
                            Task task2 = timeslot2.getTask();

                            if (task1 == null || task2 == null) {
                                return false;
                            }

                            // 检查优先级：task1优先级高于task2
                            if (task1.getPriority() <= task2.getPriority()) {
                                return false;
                            }

                            // 检查完成时间：高优先级订单完成时间晚于低优先级订单
                            if (timeslot1.getEndTime() == null || timeslot2.getEndTime() == null) {
                                return false;
                            }

                            return timeslot1.getEndTime().isAfter(timeslot2.getEndTime());
                        })
                )
                .penalize(HardSoftScore.ONE_SOFT, (timeslot1, timeslot2) -> {
                    // 计算惩罚：优先级差异越大，时间差异越大，惩罚越大
                    Task task1 = timeslot1.getTask();
                    Task task2 = timeslot2.getTask();

                    int priorityDiff = task1.getPriority() - task2.getPriority();

                    // 计算时间差异（分钟）
                    long timeDiffMinutes = Duration.between(timeslot2.getEndTime(), timeslot1.getEndTime()).toMinutes();

                    // 惩罚 = 优先级差异 * 时间差异
                    return (int) (priorityDiff * timeDiffMinutes);
                })
                .asConstraint("订单优先级约束");
    }

    /**
     * 软约束4: 平衡机器负载
     * 尽量让所有机器的工作负载均衡
     */
    public Constraint balanceMachineLoadConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null && timeslot.getDuration() > 0)
                .groupBy(
                        timeslot -> timeslot.getWorkCenter(),
                        ConstraintCollectors.sum(timeslot -> timeslot.getDuration())
                )
                .penalize(HardSoftScore.ONE_SOFT, (workCenter, totalDuration) -> {
                    // 这里需要计算所有工作中心的平均负载，然后计算当前工作中心与平均负载的差异
                    // 由于Constraint Streams的限制，我们无法在单个约束中计算全局平均值
                    // 因此采用简化方法：惩罚总工作时间过大的工作中心
                    // 工作时间越长，惩罚越大（平方惩罚以鼓励均衡）
                    return totalDuration * totalDuration / 1000; // 除以1000避免数值过大
                })
                .asConstraint("平衡机器负载");
    }

    /**
     * 软约束5: 最小化同一工序的时间片间隔
     * 同一工序的多个时间片应该连续安排
     */
    public Constraint minimizeProcedureGapConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getProcedure() != null && timeslot.getStartTime() != null && timeslot.getEndTime() != null)
                .join(Timeslot.class,
                        Joiners.equal(timeslot -> timeslot.getProcedure().getId()),
                        Joiners.filtering((timeslot1, timeslot2) -> {
                            // 确保是不同的时间槽
                            if (timeslot1.getId().equals(timeslot2.getId())) {
                                return false;
                            }

                            // 检查时间片是否连续：timeslot1在timeslot2之前结束，且两者之间有时间间隔
                            if (timeslot1.getEndTime() == null || timeslot2.getStartTime() == null) {
                                return false;
                            }

                            // timeslot1在timeslot2之前结束
                            if (!timeslot1.getEndTime().isBefore(timeslot2.getStartTime())) {
                                return false;
                            }

                            return true;
                        })
                )
                .penalize(HardSoftScore.ONE_SOFT, (timeslot1, timeslot2) -> {
                    // 计算两个时间片之间的间隔（分钟）
                    long gapMinutes = Duration.between(timeslot1.getEndTime(), timeslot2.getStartTime()).toMinutes();

                    // 间隔越大，惩罚越大
                    return (int) gapMinutes;
                })
                .asConstraint("最小化工序时间片间隔");
    }
}
