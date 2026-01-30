package com.upec.factoryscheduling.mes.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderTaskDTO implements Serializable {

    private String taskNo;

    private String orderNo;

    private String routeSeq;

    private String planQuantity;

    private String taskStatus;

    private String factStartDate;

    private String factEndDate;

    private String createUser;

    private String createDate;

    private String planStartDate;

    private String planEndDate;

    private String oldTaskNo;

    private String lockedUser;

    private String lockedDate;

    private String beforeTaskStatus;

    private String lockedRemark;

    private String mark;

    private String contractNum;

    private String productCode;

    private String productName;

}
