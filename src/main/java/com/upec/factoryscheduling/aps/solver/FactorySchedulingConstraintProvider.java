package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sum;

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
    private static final int HARD_WEIGHT_CRITICAL = 100000;  // 关键硬约束
    private static final int HARD_WEIGHT_HIGH = 10000;       // 高优先级硬约束
    private static final int HARD_WEIGHT_MEDIUM = 1000;      // 中等优先级硬约束

    // 中等约束权重 - 重要但可违反的约束
    private static final int MEDIUM_WEIGHT_HIGH = 1000;      // 高优先级中约束
    private static final int MEDIUM_WEIGHT_NORMAL = 100;     // 普通中约束

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

                // ========== 中等约束 (MEDIUM) - 重要但可适度违反 ==========
                procedureDependencyOrder(factory),               // 工序依赖顺序
                parallelProcedureSynchronization(factory),       // 并行工序同步

                // ========== 软约束 (SOFT) - 优化目标 ==========
                // 时间优化
                closeToPlannedStartDate(factory),                // 接近计划开始日期
                closeToPlannedEndDate(factory),                  // 接近计划结束日期
                earlyCompletionReward(factory),                  // 奖励早完成

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
     * 中等约束1: 工序依赖顺序
     * 后续工序必须在前置工序完成后才能开始
     * 通过nextProcedureNo确定依赖关系
     */
    protected Constraint procedureDependencyOrder(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getProcedure() != null
                        && timeslot.getProcedure().getNextProcedureNo() != null
                        && !timeslot.getProcedure().getNextProcedureNo().isEmpty()
                        && timeslot.getMaintenance() != null)
                .join(Timeslot.class,
                        // 同一任务
                        Joiners.equal(t -> t.getProcedure().getTask().getTaskNo(),
                                t -> t.getProcedure().getTask().getTaskNo()),
                        // 下一工序
                        Joiners.filtering((current, next) ->
                                next.getProcedure() != null
                                        && current.getProcedure().getNextProcedureNo()
                                        .contains(next.getProcedure().getProcedureNo())))
                .filter((current, next) -> {
                    if (next.getMaintenance() == null) return false;

                    // 找到当前工序的最后一个时间槽
                    boolean isLastSlot = current.getIndex() == current.getTotal() - 1;
                    // 找到下一工序的第一个时间槽
                    boolean isFirstSlot = next.getIndex() == 0;

                    if (!isLastSlot || !isFirstSlot) return false;

                    // 下一工序的开始日期不能早于或等于当前工序的结束日期
                    return !next.getMaintenance().getDate()
                            .isAfter(current.getMaintenance().getDate());
                })
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        (current, next) -> {
                            long daysViolation = ChronoUnit.DAYS.between(
                                    next.getMaintenance().getDate(),
                                    current.getMaintenance().getDate()) + 1;
                            return (int) daysViolation * MEDIUM_WEIGHT_HIGH;
                        })
                .asConstraint("中约束: 工序依赖顺序");
    }

    /**
     * 中等约束2: 并行工序同步
     * 如果多个工序可以并行执行,它们可以在同一天开始
     * 但它们共同的后续工序必须等所有并行工序都完成后才能开始
     */
    protected Constraint parallelProcedureSynchronization(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getProcedure() != null
                        && timeslot.getProcedure().isParallel()
                        && timeslot.getMaintenance() != null
                        && timeslot.getIndex() == timeslot.getTotal() - 1) // 最后一个时间槽
                .join(Timeslot.class,
                        // 同一任务
                        Joiners.equal(t -> t.getProcedure().getTask().getTaskNo(),
                                t -> t.getProcedure().getTask().getTaskNo()),
                        // 不同工序
                        Joiners.lessThan(t -> t.getProcedure().getProcedureNo(),
                                t -> t.getProcedure().getProcedureNo()),
                        // 也是并行工序
                        Joiners.filtering((p1, p2) -> p2.getProcedure().isParallel()
                                && p2.getIndex() == p2.getTotal() - 1))
                .join(Timeslot.class,
                        // 同一任务
                        Joiners.equal((p1, p2) -> p1.getProcedure().getTask().getTaskNo(),
                                t -> t.getProcedure().getTask().getTaskNo()),
                        // 是两个并行工序的后续工序
                        Joiners.filtering((p1, p2, next) ->
                                next.getProcedure() != null
                                        && p1.getProcedure().getNextProcedureNo() != null
                                        && p2.getProcedure().getNextProcedureNo() != null
                                        && p1.getProcedure().getNextProcedureNo()
                                        .contains(next.getProcedure().getProcedureNo())
                                        && p2.getProcedure().getNextProcedureNo()
                                        .contains(next.getProcedure().getProcedureNo())
                                        && next.getIndex() == 0)) // 第一个时间槽
                .filter((p1, p2, next) -> {
                    if (next.getMaintenance() == null
                            || p1.getMaintenance() == null
                            || p2.getMaintenance() == null) {
                        return false;
                    }

                    // 下一工序的开始日期必须晚于所有并行工序的结束日期
                    LocalDate maxParallelEnd = p1.getMaintenance().getDate()
                            .isAfter(p2.getMaintenance().getDate())
                            ? p1.getMaintenance().getDate()
                            : p2.getMaintenance().getDate();

                    return !next.getMaintenance().getDate().isAfter(maxParallelEnd);
                })
                .penalize(HardMediumSoftScore.ONE_MEDIUM,
                        (p1, p2, next) -> MEDIUM_WEIGHT_NORMAL)
                .asConstraint("中约束: 并行工序同步");
    }

    // ========================================================================
    // 软约束实现 - 优化目标
    // ========================================================================

    /**
     * 软约束1: 接近计划开始日期
     * 任务的第一个工序的第一个时间槽应尽量接近计划开始日期
     */
    protected Constraint closeToPlannedStartDate(ConstraintFactory factory) {
        return factory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getMaintenance() != null
                        && timeslot.getProcedure() != null
                        && timeslot.getProcedure().getTask() != null
                        && timeslot.getProcedure().getTask().getPlanStartDate() != null
                        && timeslot.getProcedure().getProcedureNo() == 1
                        && timeslot.getIndex() == 0)
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
                        && timeslot.getIndex() > 0
                        && !isOutsourcingProcedure(timeslot)) // 外协已有硬约束
                .join(Timeslot.class,
                        // 同一工序
                        Joiners.equal(t -> t.getProcedure().getId()),
                        // 前一个时间槽
                        Joiners.equal(t -> t.getIndex() - 1, Timeslot::getIndex))
                .filter((current, previous) -> previous.getMaintenance() != null)
                .reward(HardMediumSoftScore.ONE_SOFT,
                        (current, previous) -> {
                            long gap = ChronoUnit.DAYS.between(
                                    previous.getMaintenance().getDate(),
                                    current.getMaintenance().getDate());

                            // 连续天数给予高奖励
                            if (gap == 1) return SOFT_WEIGHT_HIGH * 3;
                            if (gap == 2) return SOFT_WEIGHT_HIGH;
                            if (gap <= 5) return SOFT_WEIGHT_NORMAL;

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
