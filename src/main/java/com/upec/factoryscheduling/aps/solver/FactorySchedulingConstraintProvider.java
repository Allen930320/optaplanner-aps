package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.xkzhangsan.time.utils.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.impl.util.CollectionUtils;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sum;


@Slf4j
@Component
public class FactorySchedulingConstraintProvider implements ConstraintProvider, Serializable {
    private static final long serialVersionUID = 1L;

    // 性能优化：缓存常量值
    private static final String STATUS_UNAVAILABLE = "N";
    private static final int MINUTES_PER_DAY = 480;
    private static final int PLANNING_HORIZON_DAYS = 30;
    private static final int AVERAGE_DAILY_LOAD = MINUTES_PER_DAY * PLANNING_HORIZON_DAYS;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // 硬约束 - 按重要性和计算复杂度排序
                workCenterMatch(constraintFactory),
                orderDateConstraint(constraintFactory),
                machineCapacityConstraint(constraintFactory),
                noOverlappingTimeslots(constraintFactory),
                procedureSequenceConstraint(constraintFactory),
                procedureSliceSequenceConstraint(constraintFactory),

                // 软约束 - 按影响力排序
                orderPriorityConstraint(constraintFactory),
                encourageEarlyStart(constraintFactory),
                minimizeMakespan(constraintFactory),
                minimizeSliceInterval(constraintFactory),
//                balanceMachineLoad(constraintFactory)
        };
    }

    /**
     * 硬约束1: 工作中心约束 - 优化版
     * 提前过滤null值,减少不必要的比较
     */
    protected Constraint workCenterMatch(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> {
                    if (timeslot.getMaintenance() == null || timeslot.getWorkCenter() == null) {
                        return false;
                    }
                    // 直接比较ID而不是对象,性能更好
                    return !timeslot.getMaintenance().getWorkCenter().getId().equals(timeslot.getWorkCenter().getId());
                }).penalize(HardSoftScore.ONE_HARD).asConstraint("工作中心不匹配");
    }

    /**
     * 硬约束2: 机器容量约束 - 优化版
     * 使用更高效的groupBy和filter组合
     */
    protected Constraint machineCapacityConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null && timeslot.getDuration() > 0)
                .groupBy(Timeslot::getMaintenance, sum(Timeslot::getDuration)
                ).filter((maintenance, totalDuration) -> totalDuration > maintenance.getCapacity())
                .penalize(HardSoftScore.ONE_HARD, (maintenance, totalDuration) -> totalDuration - maintenance.getCapacity()).asConstraint("机器容量超限");
    }

    /**
     * 硬约束3: 工序顺序约束
     * 当前工序完成时间必须早于下一道工序开始时间
     * 当存在并行工序时,并行工序开始时间可以一致
     * 但是当工序从并行工序转为串行工序时,其开始时间必须晚于上一道工序的最晚结束时间
     */
    protected Constraint procedureSequenceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class,
                        Joiners.equal(timeslot -> timeslot.getTask().getTaskNo(), timeslot -> timeslot.getTask().getTaskNo()),
                        Joiners.filtering((t1,t2)-> CollectionUtil.isNotEmpty(t1.getProcedure().getNextProcedure())&&t1.getProcedure().getNextProcedure().contains(t2.getProcedure()))
        ).filter((t1,t2)-> t1.getEndTime().isAfter(t2.getStartTime())).penalize(HardSoftScore.ONE_HARD).asConstraint("工序顺序违反");
    }

    /**
     * 硬约束3.1: 同一工序的时间片顺序约束
     * 同一工序的多个时间片必须按照index顺序执行
     */
    protected Constraint procedureSliceSequenceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getTotal() == 1)
                .join(Timeslot.class,
                        Joiners.equal(t -> t.getProcedure().getId(), t -> t.getProcedure().getId()),
                        Joiners.filtering((slice1, slice2) -> slice1.getIndex() + 1 == slice2.getIndex()))
                .filter((slice1, slice2) ->
                        slice2.getStartTime() != null && slice1.getEndTime().isAfter(slice2.getStartTime()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("工序分片顺序违反");
    }

    /**
     * 硬约束4: 订单日期约束
     * 当订单,任务实体中存在factStartDate不为空时,工序中的startTime不为空时,
     * 该规划必须按照此订单的实际开始时间为初始工序的开始时间
     */
    protected Constraint orderDateConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> {
                    return !(timeslot.getMaintenance() != null && timeslot.getProcedure().getStartTime() != null &&
                            timeslot.getStartTime().toLocalDate().equals(timeslot.getProcedure().getStartTime().toLocalDate())
                            && timeslot.getProcedureIndex() == 1);
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("违反订单实际开始日期");
    }

    /**
     * 硬约束5: 时间重叠约束
     * 同一台机器在同一时间只能处理一个时间槽
     */
    protected Constraint noOverlappingTimeslots(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null
                        && timeslot.getEndTime() != null
                        && timeslot.getWorkCenter() != null)
                .join(Timeslot.class,
                        Joiners.equal(Timeslot::getWorkCenter),
                        Joiners.lessThan(Timeslot::getId))
                .filter((t1, t2) -> t2.getStartTime() != null
                        && t2.getEndTime() != null
                        && t1.overlapsWith(t2))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("时间槽重叠");
    }

    /**
     * 软约束1: 最小化总完成时间(Makespan)
     * 尽量减少从第一个工序开始到最后一个工序完成的总时间
     */
    protected Constraint minimizeMakespan(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getEndTime() != null)
                .penalize(HardSoftScore.ONE_SOFT,
                        timeslot -> {
                            // 计算相对于今天的延迟天数
                            LocalDateTime now = LocalDateTime.now();
                            long daysDelay = Duration.between(now, timeslot.getEndTime()).toDays();
                            return (int) Math.max(0, daysDelay);
                        })
                .asConstraint("最小化总完成时间");
    }

    /**
     * 软约束2: 鼓励提前开始
     * 订单越早开始越好,特别是高优先级订单
     */
    protected Constraint encourageEarlyStart(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null
                        && timeslot.getTask() != null
                        && timeslot.getTask().getPlanStartDate() != null)
                .penalize(HardSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDateTime planStart = timeslot.getTask().getPlanStartDate().atStartOfDay();
                            LocalDateTime actualStart = timeslot.getStartTime();
                            long daysLate = Duration.between(planStart, actualStart).toDays();
                            return (int) Math.max(0, daysLate);
                        })
                .asConstraint("鼓励提前开始");
    }

    /**
     * 软约束3: 订单优先级约束
     * 高优先级订单应该在低优先级订单之前完成
     */
    protected Constraint orderPriorityConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getPriority() != null
                        && timeslot.getEndTime() != null)
                .penalize(HardSoftScore.ONE_SOFT,
                        timeslot -> {
                            // 优先级越低(数字越大),惩罚越大
                            int priority = timeslot.getPriority();
                            long daysFromNow = Duration.between(
                                    LocalDateTime.now(),
                                    timeslot.getEndTime()).toDays();
                            return priority * (int) Math.max(0, daysFromNow);
                        })
                .asConstraint("考虑订单优先级");
    }

    /**
     * 软约束4: 平衡机器负载
     * 尽量让所有机器的工作负载均衡
     */
    protected Constraint balanceMachineLoad(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null)
                .groupBy(timeslot -> timeslot.getWorkCenter().getWorkCenterCode(),
                        sum(Timeslot::getDuration))
                .penalize(HardSoftScore.ONE_SOFT,
                        (workCenter, totalDuration) -> {
                            // 对超过平均负载的部分进行惩罚
                            int averageLoad = 480 * 30; // 假设每天480分钟，30天
                            return Math.max(0, totalDuration - averageLoad) / 100;
                        })
                .asConstraint("平衡机器负载");
    }

    /**
     * 软约束5: 最小化同一工序的时间片间隔
     * 同一工序的多个时间片应该连续安排,减少等待时间
     */
    protected Constraint minimizeSliceInterval(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getTotal() > 1
                        && timeslot.getIndex() < timeslot.getTotal()
                        && timeslot.getEndTime() != null)
                .join(Timeslot.class,
                        Joiners.equal(t -> t.getProcedure().getId(), t -> t.getProcedure().getId()),
                        Joiners.filtering((slice1, slice2) ->
                                slice1.getIndex() + 1 == slice2.getIndex()))
                .filter((slice1, slice2) -> slice2.getStartTime() != null)
                .penalize(HardSoftScore.ONE_SOFT,
                        (slice1, slice2) -> {
                            // 计算两个分片之间的间隔(分钟)
                            long intervalMinutes = Duration.between(
                                    slice1.getEndTime(),
                                    slice2.getStartTime()).toDays();
                            return (int) Math.max(0, intervalMinutes);
                        })
                .asConstraint("最小化分片间隔");
    }
}
