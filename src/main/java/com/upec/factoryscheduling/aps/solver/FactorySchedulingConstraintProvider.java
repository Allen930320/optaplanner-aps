package com.upec.factoryscheduling.aps.solver;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * 工厂调度约束提供器
 * <p>实现OptaPlanner的ConstraintProvider接口，定义了工厂调度问题中的所有约束条件：</p>
 * <ul>
 *   <li>硬约束（Hard Constraints）：必须满足的规则，违反会导致解决方案不可行</li>
 *   <li>软约束（Soft Constraints）：应当尽量满足的规则，违反会降低解决方案质量</li>
 * </ul>
 * <p>这些约束条件共同构成了工厂调度优化的评估体系，指导OptaPlanner找到最佳调度方案。</p>
 */
@Slf4j  // Lombok注解，提供日志记录功能
@Component  // Spring组件，使此类可被自动注入
public class FactorySchedulingConstraintProvider implements ConstraintProvider {
    /**
     * 定义所有调度约束条件
     * <p>返回约束数组，包含系统中所有的调度规则，分为以下几类：</p>
     * <ul>
     *   <li>核心约束：保证调度的基本可行性</li>
     *   <li>资源约束：处理设备维护等资源限制</li>
     *   <li>优化约束：提高调度方案的整体质量</li>
     *   <li>分片约束：处理工序分片执行的特殊规则</li>
     * </ul>
     *
     * @param constraintFactory OptaPlanner提供的约束工厂，用于构建约束条件
     * @return 约束数组，包含所有定义的调度规则
     */
    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // 硬约束 - 必须满足
                workCenterMaintenanceAllocationConstraint(constraintFactory),
//                procedureSequenceConstraint(constraintFactory),
//                procedureSliceSequenceConstraint(constraintFactory),
//                workCenterConflict(constraintFactory),
//                workCenterMaintenanceConflict(constraintFactory),
//                fixedStartTimeConstraint(constraintFactory),
//                sameDayOrderProcedureMachineConflict(constraintFactory),
                // 软约束 - 优化目标
//                maximizeOrderPriority(constraintFactory),
//                maximizeMachineUtilization(constraintFactory),
//                minimizeMakespan(constraintFactory),
//                orderStartDateProximity(constraintFactory),
//                procedureSlicePreferContinuous(constraintFactory)
        };
    }

    /**
     * 工作中心维护分配约束 - 硬约束
     * 确保时间槽分配的工作中心维护计划与工作中心匹配
     */
    private Constraint workCenterMaintenanceAllocationConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot ->  timeslot.getWorkCenter() != null)
                .filter(this::workCenterProximity)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Work Center Maintenance Allocation Constraint");
    }


    private boolean workCenterProximity(Timeslot timeslot) {
        return (timeslot.getMaintenance() != null && timeslot.getWorkCenter() != null
                && timeslot.getMaintenance().getWorkCenter() != null
                && timeslot.getMaintenance().getCapacity() != null
                && timeslot.getMaintenance().getUsageTime() != null)
                && (!timeslot.getWorkCenter().getWorkCenterCode().equals(timeslot.getMaintenance().getWorkCenter().getWorkCenterCode()))
                && (timeslot.getMaintenance().getCapacity().compareTo(timeslot.getMaintenance().getUsageTime()) >= 0);
    }

    /**
     * 工序顺序约束 - 硬约束
     * 确保有前后依赖关系的工序按正确顺序执行
     */
    private Constraint procedureSequenceConstraint(ConstraintFactory constraintFactory) {
        // 确保两个时间槽都有有效的开始时间
        return constraintFactory.forEachUniquePair(Timeslot.class)
                .filter((timeslot1, timeslot2) -> timeslot1.getTask() != null && timeslot2.getTask() != null
                        && timeslot1.getStartTime() != null && timeslot2.getStartTime() != null
                        && timeslot1.getTask().getTaskNo().equals(timeslot2.getTask().getTaskNo())
                        && timeslot1.getProcedureIndex() < timeslot2.getProcedureIndex()
                        && procedureStartDateProximity(timeslot1, timeslot2))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Procedure sequence constraint");
    }


    private boolean procedureStartDateProximity(Timeslot t1, Timeslot t2) {
        return t1.getStartTime() != null && t2.getStartTime() != null && (t2.getStartTime().isAfter(t1.getStartTime()));
    }

    /**
     * 工序分片顺序约束 - 硬约束
     * 确保同一工序的不同分片按索引顺序执行
     * 合并了timeslotIndexOrderConstraint和procedureSliceSequence的功能
     */
    private Constraint procedureSliceSequenceConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class)
                .filter((timeslot1, timeslot2) -> timeslot1.getProcedure() != null && timeslot2.getProcedure() != null
                        && timeslot1.getStartTime() != null && timeslot2.getStartTime() != null
                        && timeslot1.getProcedure().getId().equals(timeslot2.getProcedure().getId())
                        && procedureStartDateProximity(timeslot1, timeslot2))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Procedure slice sequence constraint");
    }

    /**
     * 工作中心冲突约束 - 硬约束
     * 确保同一工作中心在同一时间只能执行一个工序
     * 优化：同一任务和同一工序的时间槽，同一天的Maintenance只能被相同的工序分配一次
     */
    private Constraint workCenterConflict(ConstraintFactory constraintFactory) {
        // 合并两个约束逻辑：基本工作中心冲突检查 + 同一天Maintenance限制
        return constraintFactory.forEachUniquePair(Timeslot.class)
                .filter((timeslot1, timeslot2) -> {
                    // 确保两个时间槽都有有效的工作中心和开始时间
                    if (timeslot1.getWorkCenter() == null || timeslot2.getWorkCenter() == null
                            || timeslot1.getStartTime() == null || timeslot2.getStartTime() == null
                            || timeslot1.getId().equals(timeslot2.getId())) {
                        return false;
                    }

                    // 检查基本工作中心冲突（同一工作中心且时间重叠）
                    boolean sameWorkCenter = timeslot1.getWorkCenter().getId().equals(timeslot2.getWorkCenter().getId());
                    LocalDateTime end1 = timeslot1.getEndTime() != null ? timeslot1.getEndTime() : timeslot1.getStartTime().plusMinutes((long) (timeslot1.getDuration() * 60));
                    LocalDateTime end2 = timeslot2.getEndTime() != null ? timeslot2.getEndTime() : timeslot2.getStartTime().plusMinutes((long) (timeslot2.getDuration() * 60));
                    boolean timeOverlap = !(timeslot1.getStartTime().isAfter(end2) || timeslot2.getStartTime().isAfter(end1));
                    boolean basicConflict = sameWorkCenter && timeOverlap;

                    // 检查同一天Maintenance冲突（同一任务、同一工序，使用了同一天的Maintenance）
                    boolean sameTask = timeslot1.getTask() != null && timeslot2.getTask() != null
                            && timeslot1.getTask().getTaskNo().equals(timeslot2.getTask().getTaskNo());
                    boolean sameProcedure = timeslot1.getProcedure() != null && timeslot2.getProcedure() != null
                            && timeslot1.getProcedure().getId().equals(timeslot2.getProcedure().getId());
                    boolean sameDayMaintenance = timeslot1.getMaintenance() != null && timeslot2.getMaintenance() != null
                            && timeslot1.getMaintenance().getDate() != null
                            && timeslot2.getMaintenance().getDate() != null
                            && timeslot1.getMaintenance().getDate().equals(timeslot2.getMaintenance().getDate());
                    boolean maintenanceConflict = sameTask && sameProcedure && sameDayMaintenance;

                    // 只要违反任一约束，就返回true
                    return basicConflict || maintenanceConflict;
                })
                .penalize(HardSoftScore.ONE_HARD, (timeslot1, timeslot2) -> {
                    // 确保两个时间槽都有有效的工作中心和开始时间
                    if (timeslot1.getWorkCenter() == null || timeslot2.getWorkCenter() == null
                            || timeslot1.getStartTime() == null || timeslot2.getStartTime() == null) {
                        return 0;
                    }

                    // 计算基本工作中心冲突的惩罚值
                    boolean sameWorkCenter = timeslot1.getWorkCenter().getId().equals(timeslot2.getWorkCenter().getId());
                    LocalDateTime end1 = timeslot1.getEndTime() != null ? timeslot1.getEndTime() : timeslot1.getStartTime().plusMinutes((long) (timeslot1.getDuration() * 60));
                    LocalDateTime end2 = timeslot2.getEndTime() != null ? timeslot2.getEndTime() : timeslot2.getStartTime().plusMinutes((long) (timeslot2.getDuration() * 60));
                    boolean timeOverlap = !(timeslot1.getStartTime().isAfter(end2) || timeslot2.getStartTime().isAfter(end1));
                    int basicPenalty = 0;
                    if (sameWorkCenter && timeOverlap) {
                        LocalDateTime overlapStart = timeslot1.getStartTime().isAfter(timeslot2.getStartTime()) ? timeslot1.getStartTime() : timeslot2.getStartTime();
                        LocalDateTime overlapEnd = end1.isBefore(end2) ? end1 : end2;
                        basicPenalty = (int) ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
                    }

                    // 计算同一天Maintenance冲突的惩罚值
                    boolean sameTask = timeslot1.getTask() != null && timeslot2.getTask() != null
                            && timeslot1.getTask().getTaskNo().equals(timeslot2.getTask().getTaskNo());
                    boolean sameProcedure = timeslot1.getProcedure() != null && timeslot2.getProcedure() != null
                            && timeslot1.getProcedure().getId().equals(timeslot2.getProcedure().getId());
                    boolean sameDayMaintenance = timeslot1.getMaintenance() != null && timeslot2.getMaintenance() != null
                            && timeslot1.getMaintenance().getDate() != null
                            && timeslot2.getMaintenance().getDate() != null
                            && timeslot1.getMaintenance().getDate().equals(timeslot2.getMaintenance().getDate());
                    int maintenancePenalty = 0;
                    if (sameTask && sameProcedure && sameDayMaintenance) {
                        maintenancePenalty = 100; // 固定惩罚值
                    }

                    // 返回较大的惩罚值
                    return Math.max(basicPenalty, maintenancePenalty);
                })
                .asConstraint("Work center conflict");
    }

    /**
     * 工作中心维护冲突约束 - 硬约束
     * 确保工序不安排在工作中心维护期间
     */
    private Constraint workCenterMaintenanceConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null && timeslot.getStartTime() != null)
                .join(WorkCenterMaintenance.class)
                .filter((timeslot, maintenance) -> {
                    // 检查工作中心是否相同
                    if (maintenance.getWorkCenter() == null || !maintenance.getWorkCenter().getId().equals(timeslot.getWorkCenter().getId())) {
                        return false;
                    }
                    // 检查维护状态是否为不可用
                    if (maintenance.getStatus() == null || !maintenance.getStatus().equals("n")) {
                        return false;
                    }
                    // 检查维护日期是否有效
                    if (maintenance.getDate() == null) {
                        return false;
                    }

                    // 检查时间是否重叠
                    LocalDateTime taskStart = timeslot.getStartTime();
                    LocalDateTime taskEnd = timeslot.getEndTime();
                    if (taskEnd == null) {
                        taskEnd = taskStart.plusMinutes((long) (timeslot.getDuration() * 60));
                    }
                    LocalDateTime maintenanceStart = LocalDateTime.of(maintenance.getDate(),
                            maintenance.getStartTime() != null ? maintenance.getStartTime() : LocalTime.MIN);
                    LocalDateTime maintenanceEnd = LocalDateTime.of(maintenance.getDate(),
                            maintenance.getEndTime() != null ? maintenance.getEndTime() : LocalTime.MAX);

                    // 检查时间重叠
                    return !(taskEnd.isBefore(maintenanceStart) || taskStart.isAfter(maintenanceEnd));
                })
                .penalize(HardSoftScore.ONE_HARD, (timeslot, maintenance) -> {
                    // 计算冲突时间
                    LocalDateTime taskStart = timeslot.getStartTime();
                    LocalDateTime taskEnd = timeslot.getEndTime();
                    if (taskEnd == null) {
                        taskEnd = taskStart.plusMinutes((long) (timeslot.getDuration() * 60));
                    }
                    LocalDateTime maintenanceStart = LocalDateTime.of(maintenance.getDate(),
                            maintenance.getStartTime() != null ? maintenance.getStartTime() : LocalTime.MIN);
                    LocalDateTime maintenanceEnd = LocalDateTime.of(maintenance.getDate(),
                            maintenance.getEndTime() != null ? maintenance.getEndTime() : LocalTime.MAX);

                    LocalDateTime overlapStart = taskStart.isAfter(maintenanceStart) ? taskStart : maintenanceStart;
                    LocalDateTime overlapEnd = taskEnd.isBefore(maintenanceEnd) ? taskEnd : maintenanceEnd;

                    if (overlapStart.isBefore(overlapEnd)) {
                        return (int) ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
                    }
                    return 0;
                })
                .asConstraint("Work center maintenance conflict");
    }

    /**
     * 固定开始时间约束 - 硬约束
     * 确保有固定开始时间的工序按计划执行
     */
    private Constraint fixedStartTimeConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null && (
                        // 检查订单是否有固定开始时间
                        (timeslot.getOrder() != null && timeslot.getOrder().getFactStartDate() != null) ||
                                // 检查工序是否有固定开始时间或已完成
                                (timeslot.getProcedure() != null && (
                                        timeslot.getProcedure().getStartTime() != null ||
                                                timeslot.getProcedure().getEndTime() != null
                                ))
                ))
                .penalize(HardSoftScore.ONE_HARD, timeslot -> {
                    int penalty = 0;

                    // 检查订单固定开始时间
                    if (timeslot.getOrder() != null && timeslot.getOrder().getFactStartDate() != null) {
                        LocalDateTime factStartDate = timeslot.getOrder().getFactStartDate();
                        LocalDateTime plannedStartTime = timeslot.getStartTime();
                        if (!factStartDate.equals(plannedStartTime)) {
                            // 计算时间偏差（分钟）作为惩罚
                            long deviationMinutes = Math.abs(ChronoUnit.MINUTES.between(factStartDate, plannedStartTime));
                            penalty += (int) deviationMinutes;
                        }
                    }

                    // 检查工序固定开始时间
                    if (timeslot.getProcedure() != null && timeslot.getProcedure().getStartTime() != null) {
                        LocalDateTime procedureStartTime = timeslot.getProcedure().getStartTime();
                        LocalDateTime plannedStartTime = timeslot.getStartTime();
                        if (!procedureStartTime.equals(plannedStartTime)) {
                            // 计算时间偏差（分钟）作为惩罚
                            long deviationMinutes = Math.abs(ChronoUnit.MINUTES.between(procedureStartTime, plannedStartTime));
                            penalty += (int) deviationMinutes;
                        }
                    }

                    // 检查工序是否已完成（不应再被规划）
                    if (timeslot.getProcedure() != null && timeslot.getProcedure().getEndTime() != null) {
                        penalty += 100; // 已完成的工序不应再被规划，给予较高惩罚
                    }

                    return penalty;
                })
                .asConstraint("Fixed start time constraint");
    }

    /**
     * 同天同订单同工序同机器约束 - 硬约束
     * 确保同天同订单同工序同机器不能同时被安排两次
     */
    private Constraint sameDayOrderProcedureMachineConflict(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class)
                .filter((timeslot1, timeslot2) -> {
                    // 确保两个时间槽都有有效的订单、工序、工作中心和开始时间
                    if (timeslot1.getOrder() == null || timeslot2.getOrder() == null
                            || timeslot1.getProcedure() == null || timeslot2.getProcedure() == null
                            || timeslot1.getWorkCenter() == null || timeslot2.getWorkCenter() == null
                            || timeslot1.getStartTime() == null || timeslot2.getStartTime() == null) {
                        return false;
                    }
                    
                    // 检查是否为同天、同订单、同工序、同机器
                    boolean sameDay = timeslot1.getStartTime().toLocalDate().equals(timeslot2.getStartTime().toLocalDate());
                    boolean sameOrder = timeslot1.getOrder().getOrderNo().equals(timeslot2.getOrder().getOrderNo());
                    boolean sameProcedure = timeslot1.getProcedure().getId().equals(timeslot2.getProcedure().getId());
                    boolean sameMachine = timeslot1.getWorkCenter().getId().equals(timeslot2.getWorkCenter().getId());
                    
                    // 检查时间是否重叠
                    LocalDateTime end1 = timeslot1.getEndTime() != null ? timeslot1.getEndTime() : timeslot1.getStartTime().plusMinutes((long) (timeslot1.getDuration() * 60));
                    LocalDateTime end2 = timeslot2.getEndTime() != null ? timeslot2.getEndTime() : timeslot2.getStartTime().plusMinutes((long) (timeslot2.getDuration() * 60));
                    boolean timeOverlap = !(timeslot1.getStartTime().isAfter(end2) || timeslot2.getStartTime().isAfter(end1));

                    // 条件：同天、同订单、同工序、同机器，且时间重叠
                    return sameDay && sameOrder && sameProcedure && sameMachine && timeOverlap;
                })
                .penalize(HardSoftScore.ONE_HARD, (timeslot1, timeslot2) -> {
                    // 对于硬约束，只要有重叠就给予固定惩罚
                    return 1;
                })
                .asConstraint("Same day, same order, same procedure, same machine conflict");
    }


    /**
     * 最大化订单优先级 - 软约束
     * 优先处理高优先级订单
     */
    private Constraint maximizeOrderPriority(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null && timeslot.getOrder() != null && timeslot.getPriority() != null)
                .reward(HardSoftScore.ONE_SOFT.multiply(10),
                        timeslot -> {
                            Integer priority = timeslot.getPriority();
                            // 优先级1（最高）给予最高奖励，优先级越高奖励越多
                            return Math.max(10, 110 - priority * 10);
                        })
                .asConstraint("Maximize order priority");
    }

    /**
     * 最大化机器利用率 - 软约束
     * 充分利用机器设备
     */
    private Constraint maximizeMachineUtilization(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getWorkCenter() != null && timeslot.getStartTime() != null)
                .join(WorkCenterMaintenance.class)
                .filter((timeslot, maintenance) -> {
                    // 确保工作中心可用且有容量信息
                    return maintenance.getWorkCenter().getId().equals(timeslot.getWorkCenter().getId())
                            && maintenance.getStatus() != null && maintenance.getStatus().equals("y")
                            && maintenance.getCapacity() != null && maintenance.getCapacity().doubleValue() > 0.0;
                })
                .groupBy((timeslot, maintenance) -> new WorkCenterUtilizationKey(
                        timeslot.getWorkCenter().getId(),
                        timeslot.getStartTime().toLocalDate(),
                        maintenance.getCapacity().doubleValue()
                ), ConstraintCollectors.sum((timeslot, maintenance) ->
                        (int) (timeslot.getDuration() * 60) // 转换为分钟
                ))
                .reward(HardSoftScore.ONE_SOFT,
                        (key, totalUsageMinutes) -> {
                            // 计算利用率百分比，最高100%
                            double capacityMinutes = key.getCapacity() * 60;
                            double utilization = ((double) totalUsageMinutes / capacityMinutes) * 100;
                            utilization = Math.min(utilization, 100); // 上限100%
                            return (int) utilization;
                        })
                .asConstraint("Maximize machine utilization");
    }

    // 辅助类，用于分组工作中心利用率数据
    @Getter
    private static class WorkCenterUtilizationKey {
        private final String workCenterId;
        private final LocalDate date;
        private final double capacity;

        public WorkCenterUtilizationKey(String workCenterId, LocalDate date, double capacity) {
            this.workCenterId = workCenterId;
            this.date = date;
            this.capacity = capacity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WorkCenterUtilizationKey that = (WorkCenterUtilizationKey) o;
            return workCenterId.equals(that.workCenterId) && date.equals(that.date);
        }

        @Override
        public int hashCode() {
            int result = workCenterId.hashCode();
            result = 31 * result + date.hashCode();
            return result;
        }
    }

    /**
     * 最小化制造周期 - 软约束
     * 尽量缩短整个生产周期
     */
    private Constraint minimizeMakespan(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null)
                // 惩罚每个任务的开始时间，鼓励尽早开始
                .penalize(HardSoftScore.ONE_SOFT,
                        timeslot -> {
                            LocalDateTime now = LocalDateTime.now();
                            LocalDateTime taskStartTime = timeslot.getStartTime();
                            // 计算从当前时间到任务开始时间的分钟数
                            long delayMinutes = ChronoUnit.MINUTES.between(now, taskStartTime);
                            // 如果任务已经开始或为过去时间，不给予惩罚
                            if (delayMinutes <= 0) {
                                return 0;
                            }
                            // 每延迟30分钟增加1点惩罚
                            return (int) (delayMinutes / 30);
                        })
                .asConstraint("Minimize makespan");
    }

    /**
     * 订单开始时间接近度 - 软约束
     * 尽量让订单按计划开始时间执行
     */
    private Constraint orderStartDateProximity(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(Timeslot.class)
                .filter(timeslot -> timeslot.getStartTime() != null && timeslot.getOrder() != null
                        && timeslot.getOrder().getPlanStartDate() != null)
                .penalize(HardSoftScore.ONE_SOFT, timeslot -> {
                    LocalDate planStartDate = timeslot.getOrder().getPlanStartDate();
                    LocalDateTime actualStartTime = timeslot.getStartTime();
                    LocalDateTime planStartDateTime = planStartDate.atStartOfDay();
                    // 计算与计划开始时间的偏差（分钟）
                    long deviationMinutes = Math.abs(ChronoUnit.MINUTES.between(actualStartTime, planStartDateTime));
                    // 每偏离一天增加1点惩罚
                    return (int) (deviationMinutes / (24 * 60));
                })
                .asConstraint("Order start date proximity");
    }

    /**
     * 工序分片连续性 - 软约束
     * 尽量让同一工序的分片连续执行
     */
    private Constraint procedureSlicePreferContinuous(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(Timeslot.class,
                        // 按工序ID分组
                        Joiners.equal(timeslot -> timeslot.getProcedure() != null ? timeslot.getProcedure().getId() : null),
                        // 按ID排序，确保只检查一次
                        Joiners.lessThan(Timeslot::getId))
                .filter((timeslot1, timeslot2) -> {
                    // 确保两个时间槽属于同一工序且有有效的开始时间和索引
                    return timeslot1.getProcedure() != null && timeslot2.getProcedure() != null
                            && timeslot1.getStartTime() != null && timeslot2.getStartTime() != null
                            && timeslot1.getIndex() != null && timeslot2.getIndex() != null
                            && timeslot1.getProcedure().getId().equals(timeslot2.getProcedure().getId())
                            && timeslot2.getIndex() == timeslot1.getIndex() + 1;
                })
                .penalize(HardSoftScore.ONE_SOFT, (timeslot1, timeslot2) -> {
                    // 计算timeslot1的结束时间
                    LocalDateTime endTime1 = timeslot1.getEndTime();
                    if (endTime1 == null) {
                        endTime1 = timeslot1.getStartTime().plusDays(1).minusMinutes(1);
                    }

                    // 只有当endTime1在startTime2之前时才计算间隔
                    if (endTime1.isBefore(timeslot2.getStartTime())) {
                        // 计算时间间隔（分钟），间隔越大惩罚越大
                        long intervalMinutes = ChronoUnit.MINUTES.between(endTime1, timeslot2.getStartTime());
                        // 每小时间隔增加1点惩罚
                        return (int) (intervalMinutes / 60);
                    } else {
                        return 0;
                    }

                })
                .asConstraint("Procedure slice prefer continuous");
    }
}
