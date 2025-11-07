package com.upec.factoryscheduling.mes.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "APS_TIMESLOT")
public class ApsTimeslot {
    @Id
    @Column(name = "ID", nullable = false, length = 30)
    private String id;

    @Column(name = "ORDER_ID", length = 30)
    private String orderId;

    @Column(name = "PROCEDURE_ID", length = 30)
    private String procedureId;

    @Column(name = "MAINTENANCE_ID", length = 30)
    private String maintenanceId;

    @Column(name = "PROBLEM_ID", length = 30)
    private String problemId;

    @Column(name = "DAILY_HOURS")
    private Long dailyHours;

    @Column(name = "DATE_TIME", length = 20)
    private String dateTime;

    @Column(name = "MANUAL", length = 10)
    private String manual;

    @Column(name = "MACHINE_ID", length = 30)
    private String machineId;

    @Column(name = "ORDER_NO", length = 30)
    private String orderNo;

}