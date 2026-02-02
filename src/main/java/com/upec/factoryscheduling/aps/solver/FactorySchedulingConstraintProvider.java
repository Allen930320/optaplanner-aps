package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.stream.*;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.*;


/**
 * 优化的工厂调度约束提供者
 * 专注于高效的约束计算和清晰的业务规则表达
 */
@Slf4j
@Component
public class FactorySchedulingConstraintProvider implements ConstraintProvider, Serializable {
    private static final long serialVersionUID = 1L;

    // ============ 常量定义 ============
    private static final String OUTSOURCING_TYPE = "ZP02";
    private static final String OUTSOURCING_WORK_CENTER = "PM10W200";
    private static final double CAPACITY_THRESHOLD = 0.90; // 90%容量阈值
    private static final int MAX_PRIORITY = 100;
    private static final int MIN_PRIORITY = 0;

    // ============ 权重定义 ============
    // 硬约束权重 - 必须满足的约束
    private static final int HARD_WEIGHT_CRITICAL = 1000;  // 关键硬约束
    private static final int HARD_WEIGHT_HIGH = 100;       // 高优先级硬约束
    private static final int HARD_WEIGHT_MEDIUM = 10;      // 中等优先级硬约束

    // 中等约束权重 - 重要但可违反的约束
    private static final int MEDIUM_WEIGHT_HIGH = 100;      // 高优先级中约束
    private static final int MEDIUM_WEIGHT_NORMAL = 10;     // 普通中约束

    // 软约束权重 - 优化目标
    private static final int SOFT_WEIGHT_HIGH = 100;         // 高优先级软约束
    private static final int SOFT_WEIGHT_NORMAL = 10;        // 普通软约束
    private static final int SOFT_WEIGHT_LOW = 1;            // 低优先级软约束

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                // ========== 硬约束 (HARD) - 必须满足 ==========
                // 基础约束
                workCenterMustMatch(factory),                    // 工作中心必须匹配
                capacityNotExceeded(factory),                    // 容量不能超过90%
                noOverlapSameProcedure(factory),                 // 同一工序同一天不能重复分配

                // 时间约束
                notBeforeCurrentDate(factory),                   // 不能早于当前日期
                maintenancePeriodAvoidance(factory),             // 避开维护期间

                // 外协工序特殊约束
                outsourcingContinuousAllocation(factory),        // 外协工序必须连续分配

                // 工序顺序约束
                timeslotSequentialOrder(factory),
                procedureDependencyOrder(factory),               // 工序依赖顺序
                parallelProcedureSynchronization(factory),       // 并行工序同步

                // ========== 软约束 (SOFT) - 优化目标 ==========
                // 时间优化
                closeToPlannedStartDate(factory),                // 接近计划开始日期
                firstProcedureCloseToToday(factory),             // 第一个工序尽量接近当天时间
                closeToPlannedEndDate(factory),                  // 接近计划结束日期
                earlyCompletionReward(factory),                  // 奖励早完成
                adjacentProcedureProximity(factory),             // 相邻工序时间接近

                // 资源优化
                continuousAllocation(factory),                   // 连续分配奖励
                highPriorityFirst(factory),                      // 高优先级优先
                capacityUtilizationOptimization(factory),        // 容量利用率优化

                // 负载均衡
                balancedWorkCenterLoad(factory)                  // 工作中心负载均衡
        };
    }

    // ========================================================================
    // 硬约束实现
    // ========================================================================

    /**
     * 硬约束1: 工作中心必须匹配
     * 时间槽分配的工作中心必须与工序要求的工作中心一致
     */
    protected Constraint workCenterMustMatch(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getProcedure() != null
                        && timeslot.getProcedure().getWorkCenter() != null
                        && !Objects.equals(
                        timeslot.getMaintenance().getWorkCenter().getId(),
                        timeslot.getProcedure().getWorkCenter().getId()))
                .penalize(HardMediumSoftScore.ONE_HARD,
                        timeslot -> HARD_WEIGHT_CRITICAL)
                .asConstraint("硬约束: 工作中心必须匹配");
    }

    /**
     * 硬约束2: 容量不能超过90%阈值
     * 每个工作中心每天的容量使用不能超过90%
     * 外协工序(PM10W200)不受此限制
     */
    protected Constraint capacityNotExceeded(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getDuration() > 0
                        && !isOutsourcingWorkCenter(timeslot))
                .groupBy(Timeslot::getMaintenance, sum(Timeslot::getDuration))
                .filter((maintenance, totalDuration) -> {
                    int totalUsage = maintenance.getUsageTime() + totalDuration;
                    int threshold = (int) (maintenance.getCapacity() * CAPACITY_THRESHOLD);
                    return totalUsage > threshold;
                })
                .penalize(HardMediumSoftScore.ONE_HARD,
                        (maintenance, totalDuration) -> {
                            int totalUsage = maintenance.getUsageTime() + totalDuration;
                            int threshold = (int) (maintenance.getCapacity() * CAPACITY_THRESHOLD);
                            int exceeded = totalUsage - threshold;
                            return exceeded * HARD_WEIGHT_HIGH;
                        })
                .asConstraint("硬约束: 容量不超90%");
    }

    /**
     * 硬约束3: 同一工序在同一天不能重复分配
     * 每个工序每天只能分配一次时间槽
     */
    protected Constraint noOverlapSameProcedure(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getProcedure() != null)
                .join(Timeslot.class,
                        // 同一工序
                        Joiners.equal(t -> t.getProcedure().getId()),
                        // 不同时间槽索引
                        Joiners.lessThan(Timeslot::getIndex),
                        // 同一天
                        Joiners.equal(t -> t.getMaintenance().getDate()))
                .penalize(HardMediumSoftScore.ONE_HARD,
                        (slot1, slot2) -> HARD_WEIGHT_CRITICAL)
                .asConstraint("硬约束: 同工序同天不重复");
    }

    /**
     * 硬约束4: 不能早于当前日期
     * 所有时间槽的分配日期不能早于当前日期
     */
    protected Constraint notBeforeCurrentDate(ConstraintFactory factory) {
        LocalDate today = LocalDate.now();
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getMaintenance().getDate().isBefore(today))
                .penalize(HardMediumSoftScore.ONE_HARD,
                        timeslot -> {
                            long daysBefore = ChronoUnit.DAYS.between(
                                    timeslot.getMaintenance().getDate(), today);
                            return (int) daysBefore * HARD_WEIGHT_MEDIUM;
                        })
                .asConstraint("硬约束: 不早于当前日期");
    }

    /**
     * 硬约束5: 避开维护期间
     * 在工作中心维护期间不能分配任务
     */
    protected Constraint maintenancePeriodAvoidance(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && "N".equals(timeslot.getMaintenance().getStatus()))
                .penalize(HardMediumSoftScore.ONE_HARD,
                        timeslot -> HARD_WEIGHT_HIGH)
                .asConstraint("硬约束: 避开维护期间");
    }

    /**
     * 硬约束6: 外协工序必须连续分配
     * 外协工序(ZP02类型且工作中心为PM10W200)的时间槽必须在连续的天数内完成
     */
    protected Constraint outsourcingContinuousAllocation(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> isOutsourcingProcedure(timeslot)
                        && timeslot.getMaintenance() != null
                        && timeslot.getIndex() > 0)
                .join(Timeslot.class,
                        // 同一工序
                        Joiners.equal(t -> t.getProcedure().getId()),
                        // 前一个时间槽
                        Joiners.equal(t -> t.getIndex() - 1, Timeslot::getIndex))
                .filter((current, previous) ->
                        previous.getMaintenance() != null
                                && ChronoUnit.DAYS.between(
                                previous.getMaintenance().getDate(),
                                current.getMaintenance().getDate()) > 1)
                .penalize(HardMediumSoftScore.ONE_HARD,
                        (current, previous) -> {
                            long gap = ChronoUnit.DAYS.between(
                                    previous.getMaintenance().getDate(),
                                    current.getMaintenance().getDate()) - 1;
                            return (int) gap * HARD_WEIGHT_HIGH;
                        })
                .asConstraint("硬约束: 外协工序必须连续");
    }

    // ========================================================================
    // 中等约束实现
    // ========================================================================

    /**
     * 硬约束: 工序依赖顺序
     * 后续工序必须在前置工序完成后才能开始
     * 通过Procedure.index
     */
    protected Constraint procedureDependencyOrder(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getProcedure() != null
                        && timeslot.getEndTime() != null
                        && timeslot.getIndex() == timeslot.getTotal()) // 前置工序的最后一个时间槽
                .join(Timeslot.class, Joiners.equal(timeslot -> timeslot.getProcedure().getLevel() + 1,
                        timeslot -> timeslot.getProcedure().getLevel()))
                .filter((previous, next) -> {
                    // 后续工序的开始日期必须在前置工序结束日期的第二天或之后
                    long daysBetween = ChronoUnit.DAYS.between(
                            previous.getMaintenance().getDate(),
                            next.getMaintenance().getDate());
                    return daysBetween < 1; // 必须至少间隔0天（同一天结束后第二天开始）
                }).penalize(HardMediumSoftScore.ONE_HARD,
                        (previous, next) -> {
                            long daysViolation = ChronoUnit.DAYS.between(
                                    next.getMaintenance().getDate(),
                                    previous.getMaintenance().getDate()) + 1;
                            return (int) Math.max(1, daysViolation) * HARD_WEIGHT_HIGH;
                        })
                .asConstraint("硬约束: 工序依赖顺序");
    }

    /**
     * 硬约束: 时间槽顺序执行
     * 同任务同工序的时间槽必须按照index顺序执行，由小到大排序
     */
    protected Constraint timeslotSequentialOrder(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getProcedure() != null)
                .join(Timeslot.class,
                        Joiners.equal(Timeslot::getProcedure, Timeslot::getProcedure),
                        // 时间槽索引较小的在前
                        Joiners.lessThan(Timeslot::getIndex, Timeslot::getIndex),
                        // 但日期较大的在前（顺序错误）
                        Joiners.filtering((timeslot1, timeslot2) -> {
                            if (timeslot2.getMaintenance() == null) return false;
                            return timeslot2.getStartTime().isBefore(timeslot1.getStartTime());
                        }))
                .penalize(HardMediumSoftScore.ONE_HARD,
                        (slot1, slot2) -> HARD_WEIGHT_CRITICAL)
                .asConstraint("硬约束: 时间槽顺序执行");
    }

    /**
     * 硬约束: 并行工序同步
     * 如果多个工序可以并行执行,它们可以在同一天开始
     * 但它们共同的后续工序必须等所有并行工序都完成后才能开始
     */
    protected Constraint parallelProcedureSynchronization(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getProcedure() != null
                        && timeslot.getMaintenance() != null
                        && timeslot.getIndex() == timeslot.getTotal() - 1) // 工序的最后一个时间槽
                .groupBy(timeslot -> timeslot.getProcedure().getTask(),
                        timeslot -> timeslot.getProcedure().getLevel(),
                        toList())
                .filter((task, level, timeslots) -> timeslots.size() > 1) // 确保有多个并行工序
                .join(Timeslot.class,
                        // 同一任务
                        Joiners.equal((task, level, timeslots) -> task,
                                timeslot -> timeslot.getProcedure().getTask()),
                        // 后续工序（level+1）
                        Joiners.equal((task, level, timeslots) -> level + 1,
                                timeslot -> timeslot.getProcedure().getLevel()),
                        // 后续工序的第一个时间槽
                        Joiners.filtering((task, level, timeslots, next) -> next.getIndex() == 1))
                .filter((task, level, timeslots, next) -> {
                    if (next.getMaintenance() == null) return false;
                    // 找到所有并行工序的最晚结束日期
                    LocalDate latestEndDate = null;
                    for (Timeslot timeslot : timeslots) {
                        if (timeslot.getMaintenance() != null) {
                            LocalDate endDate = timeslot.getMaintenance().getDate();
                            if (latestEndDate == null || endDate.isAfter(latestEndDate)) {
                                latestEndDate = endDate;
                            }
                        }
                    }
                    // 后续工序的开始日期必须在所有并行工序结束日期的第二天或之后
                    return latestEndDate != null && !next.getMaintenance().getDate().isAfter(latestEndDate.plusDays(1));
                })
                .penalize(HardMediumSoftScore.ONE_HARD,
                        (task, level, timeslots, next) -> HARD_WEIGHT_HIGH)
                .asConstraint("硬约束: 并行工序同步");
    }

    // ========================================================================
    // 软约束实现 - 优化目标
    // ========================================================================

    /**
     * 软约束1: 接近计划开始日期
     * 任务的第一个工序的第一个时间槽应尽量接近计划开始日期
     */
    protected Constraint closeToPlannedStartDate(ConstraintFactory factory) {
        LocalDate today = LocalDate.now();
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null && timeslot.getStartTime() != null && timeslot.getProcedure().getOrder().getPlanStartDate().isAfter(today))
                .groupBy(timeslot -> timeslot.getProcedure().getTask(), toList())
                .groupBy(((task, timeslots) -> timeslots.stream().min(Comparator.comparing(timeslot -> timeslot.getProcedureIndex() * 100 + timeslot.getIndex())).orElse(null)))
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDate planStart = timeslot.getProcedure().getTask().getPlanStartDate();
                            LocalDate actualStart = timeslot.getMaintenance().getDate();
                            long daysDiff = Math.abs(ChronoUnit.DAYS.between(planStart, actualStart));
                            // 距离越近奖励越高
                            if (daysDiff == 0) return SOFT_WEIGHT_HIGH * 10;
                            if (daysDiff <= 2) return SOFT_WEIGHT_HIGH * 5;
                            if (daysDiff <= 5) return SOFT_WEIGHT_HIGH * 2;
                            if (daysDiff <= 10) return SOFT_WEIGHT_NORMAL;

                            return 0;
                        })
                .asConstraint("软约束: 接近计划开始日期");
    }

    /**
     * 软约束: 第一个工序尽量接近当天时间
     * 任务的第一个工序的第一个时间槽应尽量接近当天时间
     */
    protected Constraint firstProcedureCloseToToday(ConstraintFactory factory) {
        LocalDate today = LocalDate.now();
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null && timeslot.getStartTime() != null && timeslot.getProcedure().getOrder().getPlanStartDate().isBefore(today))
                .groupBy(timeslot -> timeslot.getProcedure().getTask(), toList())
                .groupBy(((task, timeslots) -> timeslots.stream().min(Comparator.comparing(timeslot -> timeslot.getProcedureIndex() * 100 + timeslot.getIndex())).orElse(null)))
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDate scheduleDate = timeslot.getStartTime().toLocalDate();
                            long daysFromToday = ChronoUnit.DAYS.between(today, scheduleDate);
                            // 越接近今天奖励越高
                            if (daysFromToday == 0) return SOFT_WEIGHT_HIGH * 20;
                            if (daysFromToday == 1) return SOFT_WEIGHT_HIGH * 15;
                            if (daysFromToday == 2) return SOFT_WEIGHT_HIGH * 10;
                            if (daysFromToday <= 5) return SOFT_WEIGHT_HIGH * 5;
                            if (daysFromToday <= 10) return SOFT_WEIGHT_HIGH;
                            if (daysFromToday <= 15) return SOFT_WEIGHT_NORMAL;
                            return 0;
                        })
                .asConstraint("软约束: 第一个工序尽量接近当天时间");
    }

    /**
     * 软约束2: 接近计划结束日期
     * 任务的最后一个工序的最后一个时间槽应尽量接近计划结束日期
     */
    protected Constraint closeToPlannedEndDate(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getProcedure() != null
                        && timeslot.getProcedure().getTask() != null
                        && timeslot.getProcedure().getTask().getPlanEndDate() != null
                        && (timeslot.getProcedure().getNextProcedureNo() == null
                        || timeslot.getProcedure().getNextProcedureNo().isEmpty())
                        && timeslot.getIndex() == timeslot.getTotal() - 1)
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDate planEnd = timeslot.getProcedure().getTask().getPlanEndDate();
                            LocalDate actualEnd = timeslot.getMaintenance().getDate();
                            long daysDiff = Math.abs(ChronoUnit.DAYS.between(planEnd, actualEnd));
                            // 提前完成给予额外奖励
                            if (actualEnd.isBefore(planEnd)) {
                                if (daysDiff <= 3) return SOFT_WEIGHT_HIGH * 15;
                                if (daysDiff <= 7) return SOFT_WEIGHT_HIGH * 8;
                                return SOFT_WEIGHT_HIGH * 3;
                            }
                            // 按时完成
                            if (daysDiff == 0) return SOFT_WEIGHT_HIGH * 10;

                            // 轻微延迟的惩罚较小
                            if (daysDiff <= 2) return SOFT_WEIGHT_NORMAL * 5;
                            if (daysDiff <= 5) return SOFT_WEIGHT_NORMAL;

                            return 0;
                        })
                .asConstraint("软约束: 接近计划结束日期");
    }

    /**
     * 软约束3: 早完成奖励
     * 奖励提前完成的任务,越早完成奖励越高
     */
    protected Constraint earlyCompletionReward(ConstraintFactory factory) {
        LocalDate today = LocalDate.now();
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getProcedure() != null
                        && (timeslot.getProcedure().getNextProcedureNo() == null
                        || timeslot.getProcedure().getNextProcedureNo().isEmpty())
                        && timeslot.getIndex() == timeslot.getTotal() - 1)
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDate endDate = timeslot.getMaintenance().getDate();
                            long daysFromNow = ChronoUnit.DAYS.between(today, endDate);
                            // 越早完成奖励越高
                            if (daysFromNow <= 7) return SOFT_WEIGHT_HIGH * 5;
                            if (daysFromNow <= 14) return SOFT_WEIGHT_HIGH * 3;
                            if (daysFromNow <= 21) return SOFT_WEIGHT_HIGH;

                            return SOFT_WEIGHT_NORMAL;
                        })
                .asConstraint("软约束: 早完成奖励");
    }

    /**
     * 软约束4: 连续分配奖励
     * 奖励工序的时间槽在连续天数内完成
     */
    protected Constraint continuousAllocation(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getProcedure() != null
                        && timeslot.getIndex() > 0) // 不是第一个时间槽
                .join(Timeslot.class,
                        // 同一工序
                        Joiners.equal(Timeslot::getProcedure, Timeslot::getProcedure),
                        // 前一个时间槽（索引-1）
                        Joiners.equal(t -> t.getIndex() - 1, Timeslot::getIndex))
                .reward(HardMediumSoftScore.ONE_SOFT,
                        (current, previous) -> {
                            long daysBetween = ChronoUnit.DAYS.between(
                                    previous.getMaintenance().getDate(),
                                    current.getMaintenance().getDate());
                            
                            // 连续天数给予高奖励，间隔越大奖励越少
                            if (daysBetween == 1) return SOFT_WEIGHT_HIGH * 20; // 连续天
                            if (daysBetween == 2) return SOFT_WEIGHT_HIGH * 10; // 间隔1天
                            if (daysBetween == 3) return SOFT_WEIGHT_HIGH * 5;  // 间隔2天
                            if (daysBetween <= 5) return SOFT_WEIGHT_HIGH;      // 间隔3-4天
                            if (daysBetween <= 7) return SOFT_WEIGHT_NORMAL;    // 间隔5-6天
                            return 0;
                        })
                .asConstraint("软约束: 连续分配奖励");
    }

    /**
     * 软约束5: 高优先级优先
     * 优先级高的工序优先分配,优先完成
     */
    protected Constraint highPriorityFirst(ConstraintFactory factory) {
        LocalDate today = LocalDate.now();
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getPriority() != null
                        && timeslot.getPriority() > 50) // 高优先级
                .reward(HardMediumSoftScore.ONE_SOFT,
                        timeslot -> {
                            int priority = timeslot.getPriority();
                            LocalDate scheduleDate = timeslot.getMaintenance().getDate();
                            long daysFromNow = ChronoUnit.DAYS.between(today, scheduleDate);

                            // 优先级越高,越早完成,奖励越高
                            int priorityWeight = (priority - 50) * 2; // 50-100映射到0-100
                            int timeWeight = Math.max(0, 30 - (int) daysFromNow); // 越早权重越高

                            return priorityWeight * timeWeight;
                        })
                .asConstraint("软约束: 高优先级优先");
    }

    /**
     * 软约束6: 容量利用率优化
     * 优化工作中心的容量利用率,既不过载也不浪费
     * 目标利用率: 70%-85%
     */
    protected Constraint capacityUtilizationOptimization(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getDuration() > 0
                        && !isOutsourcingWorkCenter(timeslot))
                .groupBy(Timeslot::getMaintenance, sum(Timeslot::getDuration))
                .reward(HardMediumSoftScore.ONE_SOFT,
                        (maintenance, totalDuration) -> {
                            int totalUsage = maintenance.getUsageTime() + totalDuration;
                            int capacity = maintenance.getCapacity();
                            double utilization = (double) totalUsage / capacity;

                            // 最佳利用率范围: 70%-85%
                            if (utilization >= 0.70 && utilization <= 0.85) {
                                return SOFT_WEIGHT_HIGH * 5;
                            }
                            // 可接受范围: 60%-90%
                            if (utilization >= 0.60 && utilization < 0.70) {
                                return SOFT_WEIGHT_HIGH * 2;
                            }
                            if (utilization > 0.85 && utilization <= 0.90) {
                                return SOFT_WEIGHT_HIGH;
                            }
                            // 次优范围: 50%-60%
                            if (utilization >= 0.50 && utilization < 0.60) {
                                return SOFT_WEIGHT_NORMAL;
                            }

                            return 0;
                        })
                .asConstraint("软约束: 容量利用率优化");
    }

    /**
     * 软约束7: 工作中心负载均衡
     * 平衡不同工作中心的负载,避免某些工作中心过载而其他空闲
     */
    protected Constraint balancedWorkCenterLoad(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getDuration() > 0
                        && !isOutsourcingWorkCenter(timeslot))
                .groupBy(t -> t.getMaintenance().getWorkCenter().getId(),
                        t -> t.getMaintenance().getDate(),
                        sum(Timeslot::getDuration))
                .reward(HardMediumSoftScore.ONE_SOFT,
                        (workCenterId, date, totalDuration) -> {
                            // 基于每日工作时间8小时=480分钟计算
                            int dailyCapacity = 480;
                            double loadRate = (double) totalDuration / dailyCapacity;

                            // 理想负载率: 60%-80%
                            if (loadRate >= 0.60 && loadRate <= 0.80) {
                                return SOFT_WEIGHT_NORMAL * 3;
                            }
                            if (loadRate >= 0.50 && loadRate < 0.60) {
                                return SOFT_WEIGHT_NORMAL;
                            }
                            if (loadRate > 0.80 && loadRate <= 0.90) {
                                return SOFT_WEIGHT_NORMAL;
                            }
                            return 0;
                        })
                .asConstraint("软约束: 工作中心负载均衡");
    }

    /**
     * 软约束: 相邻工序时间接近
     * 相邻工序之间，下一道工序的开始时间尽量与上一道工序的结束时间相邻，越接近越好
     */
    protected Constraint adjacentProcedureProximity(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getProcedure() != null
                        && timeslot.getEndTime() != null
                        && timeslot.getIndex() == timeslot.getTotal()) // 上一道工序的最后一个时间槽
                .join(Timeslot.class,
                        Joiners.equal(timeslot -> timeslot.getProcedure().getTask(), timeslot -> timeslot.getProcedure().getTask()),
                        Joiners.equal(timeslot -> timeslot.getProcedure().getLevel() + 1, timeslot -> timeslot.getProcedure().getLevel()))
                .filter((previous, next) -> {
                    // 下一道工序的第一个时间槽
                    return next.getIndex() == 1;
                }).penalize(HardMediumSoftScore.ONE_MEDIUM,
                        (previous, next) -> {
                            long daysBetween = ChronoUnit.DAYS.between(
                                    previous.getMaintenance().getDate(),
                                    next.getMaintenance().getDate());
                            // 间隔越大惩罚越重，相邻天不惩罚
                            if (daysBetween == 1) return 0; // 相邻天，无惩罚
                            if (daysBetween == 2) return SOFT_WEIGHT_LOW; // 间隔1天，轻微惩罚
                            if (daysBetween == 3) return SOFT_WEIGHT_NORMAL; // 间隔2天，普通惩罚
                            if (daysBetween == 4) return SOFT_WEIGHT_NORMAL * 2; // 间隔3天，较重惩罚
                            if (daysBetween == 5) return SOFT_WEIGHT_HIGH; // 间隔4天，严重惩罚
                            if (daysBetween <= 7) return SOFT_WEIGHT_HIGH * 2; // 间隔5-6天，更严重惩罚
                            return SOFT_WEIGHT_HIGH * 5; // 间隔超过7天，最严重惩罚
                        })
                .asConstraint("软约束: 相邻工序时间接近");
    }

    // ========================================================================
    // 辅助方法
    // ========================================================================

    /**
     * 判断是否为外协工序
     */
    private boolean isOutsourcingProcedure(Timeslot timeslot) {
        return timeslot.getProcedure() != null
                && OUTSOURCING_TYPE.equals(timeslot.getProcedure().getProcedureType())
                && timeslot.getProcedure().getWorkCenter() != null
                && OUTSOURCING_WORK_CENTER.equals(
                timeslot.getProcedure().getWorkCenter().getWorkCenterCode());
    }

    /**
     * 判断是否为外协工作中心
     */
    private boolean isOutsourcingWorkCenter(Timeslot timeslot) {
        return timeslot.getProcedure() != null
                && timeslot.getProcedure().getWorkCenter() != null
                && OUTSOURCING_WORK_CENTER.equals(
                timeslot.getProcedure().getWorkCenter().getWorkCenterCode());
    }
}
