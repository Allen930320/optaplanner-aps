package com.upec.factoryscheduling.aps.entity;

import com.upec.factoryscheduling.aps.solution.WorkCenterMaintenanceVariableListener;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.ShadowVariable;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "work_center_maintenance")
@Data
@Getter
@Setter
@PlanningEntity
public class WorkCenterMaintenance {

    @Id
    @PlanningId
    private String id;

    @OneToOne(fetch = FetchType.EAGER)
    private WorkCenter workCenter;

    private int year;

    private LocalDate date;

    private BigDecimal capacity;

    private String status;

    private String description;

    private LocalTime startTime;

    private LocalTime endTime;

    @ShadowVariable(variableListenerClass = WorkCenterMaintenanceVariableListener.class,sourceVariableName =
            "maintenance",sourceEntityClass = Timeslot.class)
    private BigDecimal usageTime;

    /**
     * 检查是否还有可用容量
     */
    public boolean hasAvailableCapacity() {
        BigDecimal capacity = this.capacity != null ? this.capacity : BigDecimal.ZERO;
        BigDecimal usage = this.usageTime != null ? this.usageTime : BigDecimal.ZERO;
        return usage.compareTo(capacity) < 0;
    }

    /**
     * 获取剩余可用容量
     */
    public BigDecimal getRemainingCapacity() {
        BigDecimal capacity = this.capacity != null ? this.capacity : BigDecimal.ZERO;
        BigDecimal usage = this.usageTime != null ? this.usageTime : BigDecimal.ZERO;
        return capacity.subtract(usage);
    }

    /**
     * 累加使用时间
     */
    public void addUsageTime(BigDecimal duration) {
        if (this.usageTime == null) {
            this.usageTime = BigDecimal.ZERO;
        }
        if (duration != null) {
            duration = duration.multiply(BigDecimal.valueOf(60));
            this.usageTime = this.usageTime.add(duration);
        }
    }

    /**
     * 减少使用时间
     */
    public void subtractUsageTime(BigDecimal duration) {
        if (this.usageTime != null && duration != null) {
            duration = duration.multiply(BigDecimal.valueOf(60));
            this.usageTime = this.usageTime.subtract(duration);
            if (this.usageTime.compareTo(BigDecimal.ZERO) < 0) {
                this.usageTime = BigDecimal.ZERO;
            }
        }
    }

    public WorkCenterMaintenance() {
    }

    public WorkCenterMaintenance(WorkCenter workCenter, LocalDate date,  BigDecimal capacity, String description) {
        this.workCenter = workCenter;
        this.date = date;
        this.capacity = capacity;
        this.description = description;
    }

}
