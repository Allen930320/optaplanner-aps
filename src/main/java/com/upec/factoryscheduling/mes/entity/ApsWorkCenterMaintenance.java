package com.upec.factoryscheduling.mes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.io.Serializable;

@Setter
@Getter
@Entity
@Table(name = "APS_MACHINE_MAINTENANCE")
public class ApsWorkCenterMaintenance implements Serializable {
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
