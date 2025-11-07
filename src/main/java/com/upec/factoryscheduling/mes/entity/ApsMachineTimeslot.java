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
@Table(name = "APS_MACHINE_TIMESLOT")
public class ApsMachineTimeslot {
    @Id
    @Column(name = "ID", nullable = false, length = 30)
    private String id;

    @Column(name = "MACHINE_ID", length = 30)
    private String machineId;

    @Column(name = "START_TIME", length = 20)
    private String startTime;

    @Column(name = "END_TIME", length = 20)
    private String endTime;

    @Column(name = "STATUS", length = 20)
    private String status;

}