package com.upec.factoryscheduling.aps.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "APS_ROUTE_PROCEDURE")
public class RouteProcedure {

    @Id
    @Column(name = "ID", nullable = false, length = 20)
    private String id;

    @Column(name = "ROUTE_SEQ", nullable = true, length = 20)
    private String routeSeq;

    @Column(name = "PROCEDURE_NO")
    private Integer procedureNo;

    @Column(name = "PROCEDURE_CODE", nullable = true, length = 20)
    private String procedureCode;

    @Column(name = "PROCEDURE_NAME", nullable = true, length = 100)
    private String procedureName;

    @Column(name = "PROCEDURE_TYPE", nullable = true, length = 200)
    private String procedureType;

    @Column(name = "PROCEDURE_CONTENT", nullable = true, length = 1000)
    private String procedureContent;

    @Column(name = "WORK_CENTER_ID", nullable = true, length = 20)
    private String workCenterId;

    @Column(name = "MACHINE_HOURS", nullable = true, length = 10)
    private String machineHours;

    @Column(name = "HUMAN_HOURS", nullable = true, length = 10)
    private String humanHours;

    @Column(name = "DUTY_USER", nullable = true, length = 20)
    private String dutyUser;

    @Column(name = "REMARK", nullable = true, length = 200)
    private String remark;

    @Column(name = "CREATE_USER", nullable = true, length = 20)
    private String createUser;

    @Column(name = "CREATE_DATE", nullable = true, length = 20)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_USER", nullable = true, length = 20)
    private String updateUser;

    @Column(name = "UPDATE_DATE", nullable = true, length = 20)
    private LocalDateTime updateDate;

    @Column(name = "PLAN_DAYS",length = 10)
    private Integer days;
}
