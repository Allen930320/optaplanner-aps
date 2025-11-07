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
@Table(name = "APS_MAINTENANCE")
public class ApsMaintenance {
    @Id
    @Column(name = "LOCAL_DATE", nullable = false, length = 20)
    private String localDate;

    @Column(name = "CAPACITY", nullable = false, length = 20)
    private String capacity;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "START_TIME", nullable = false, length = 20)
    private String startTime;

    @Column(name = "END_TIME", nullable = false, length = 20)
    private String endTime;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

    @Column(name = "YEAR_DATE", nullable = false, length = 20)
    private String yearDate;

}