package com.upec.factoryscheduling.mes.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
@Setter
@Getter
@Entity
@Table(name = "APS_MACHINE_MAINTENANCE")
public class ApsWorkCenterMaintenance {
    // 手动添加getter和setter方法
    @Id
    @Column(name = "ID", nullable = false, length = 30)
    private String id;

    @Column(name = "WORK_CENTER_CODE", nullable = false, length = 20)
    private String workCenterCode;

    @Column(name = "LOCAL_DATE", nullable = false, length = 20)
    private String localDate;

    @Column(name = "CAPACITY", nullable = false)
    private int capacity;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "START_TIME", nullable = false, length = 20)
    private String startTime;

    @Column(name = "END_TIME", nullable = false, length = 20)
    private String endTime;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

}
