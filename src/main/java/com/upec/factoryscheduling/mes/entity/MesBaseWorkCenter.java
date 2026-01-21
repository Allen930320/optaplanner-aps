package com.upec.factoryscheduling.mes.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "MES_BASE_WORKCENTER",  indexes = {
        @Index(name = "IDX$$_03B60003", columnList = "WORKCENTER_CODE")
})
@Data
public class MesBaseWorkCenter implements Serializable {
    @Id
    @Column(name = "SEQ", nullable = false, length = 20)
    private String seq;

    @Column(name = "WORKCENTER_CODE", length = 20)
    private String workCenterCode;

    @Column(name = "DESCRIPTION", length = 200)
    private String description;

    @Column(name = "COSTCENTER_SEQ", length = 20)
    private String costCenterSeq;

    @Column(name = "STATUS", length = 1)
    private String status;

    @Column(name = "FACTORY_SEQ", length = 20)
    private String factorySeq;

    @Column(name = "CREATEUSER", length = 20)
    private String createUser;

    @Column(name = "CREATEDATE", length = 20)
    private String createDate;

    @Column(name = "ATTRIBUTES", length = 8)
    private String attributes;

    @Column(name = "MACHINE_HOURS_COST", length = 10)
    private String machineHoursCost;

    @Column(name = "HUMAN_HOURS_COST", length = 10)
    private String humanHoursCost;

    @Column(name = "WORK_CENTER_GROUP", length = 50)
    private String workCenterGroup;

    @Column(name = "REMARK", length = 200)
    private String remark;

}
