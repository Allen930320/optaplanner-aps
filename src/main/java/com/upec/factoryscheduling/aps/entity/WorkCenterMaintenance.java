package com.upec.factoryscheduling.aps.entity;

import com.upec.factoryscheduling.common.utils.RandomFun;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "APS_WORK_CENTER_MAINTENANCE")
public class WorkCenterMaintenance implements Serializable {
    // 手动添加getter和setter方法
    @Id
    @Column(name = "ID", nullable = false, length = 30)
    private String id;

    @JoinColumn(name = "WORK_CENTER")
    @OneToOne(fetch = FetchType.EAGER)
    private WorkCenter workCenter;

    @Column(name = "CALENDAR_DATE", nullable = false, length = 20)
    private LocalDate calendarDate;

    @Column(name = "CAPACITY", nullable = false)
    private int capacity;

    @Column(name = "USAGE_TIME", nullable = false)
    private int usageTime;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "START_TIME", nullable = false, length = 20)
    private LocalDateTime startTime;

    @Column(name = "END_TIME", nullable = false, length = 20)
    private LocalDateTime endTime;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;


    public void addUsageTime(int usageTime) {
        this.usageTime += usageTime;
    }

    public void subtractUsageTime(int usageTime) {
        this.usageTime -= usageTime;
    }


    public WorkCenterMaintenance() {

    }

    public WorkCenterMaintenance(WorkCenter workCenter, LocalDate calendarDate) {
        this.workCenter = workCenter;
        this.calendarDate = calendarDate;
        this.id = RandomFun.getInstance().getRandom();
        this.status = workCenter.getStatus();
        this.capacity = workCenter.getCapacity();
        this.startTime = calendarDate.atTime(9, 0);
        this.endTime = calendarDate.atTime(18, 0);
    }
}
