package com.upec.factoryscheduling.mes.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class OrderTaskDTO implements Serializable {

    private String taskNo;

    private String orderNo;

    private String routeSeq;

    private String planQuantity;

    private String taskStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate factStartDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate factEndDate;

    private String createUser;

    private String createDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planStartDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate planEndDate;

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
