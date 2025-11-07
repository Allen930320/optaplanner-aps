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
@Table(name = "APS_ORDER")
public class ApsOrder {
    @Id
    @Column(name = "ID", nullable = false, length = 30)
    private String id;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "ORDER_NO", nullable = false, length = 20)
    private String orderNo;

    @Column(name = "PRIORITY", nullable = false)
    private Long priority;

    @Column(name = "START_DATE", nullable = false, length = 20)
    private String startDate;

    @Column(name = "END_DATE", nullable = false, length = 20)
    private String endDate;

    @Column(name = "PROBLEM_ID", length = 30)
    private String problemId;

    @Column(name = "CREATE_DATE", length = 20)
    private String createDate;

    @Column(name = "TYPE", length = 50)
    private String type;

    @Column(name = "VERSION", length = 20)
    private String version;

    @Column(name = "STATUS", length = 20)
    private String status;

}