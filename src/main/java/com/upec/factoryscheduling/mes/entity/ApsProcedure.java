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
@Table(name = "APS_PROCEDURE")
public class ApsProcedure {
    @Id
    @Column(name = "ID", nullable = false, length = 30)
    private String id;

    @Column(name = "ORDER_NO", length = 20)
    private String orderNo;

    @Column(name = "PROCEDURE_NO", length = 10)
    private String procedureNo;

    @Column(name = "MACHINE_NO", length = 10)
    private String machineNo;

    @Column(name = "NAME", length = 100)
    private String name;

    @Column(name = "DURATION")
    private Long duration;

    @Column(name = "NEXT", length = 20)
    private String next;

    @Column(name = "START_TIME", length = 20)
    private String startTime;

    @Column(name = "END_TIME", length = 20)
    private String endTime;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "CREATED_TIME", length = 20)
    private String createdTime;

}