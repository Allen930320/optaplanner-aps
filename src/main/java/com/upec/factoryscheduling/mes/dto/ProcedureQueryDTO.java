package com.upec.factoryscheduling.mes.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProcedureQueryDTO implements Serializable {


    // 合同信息
    private String contractNum;
    // 任务信息
    private String taskNo;
    private String orderNo;
    private String taskStatus;
    // 产品信息
    private String productCode;
    private String productName;
    // 工序信息
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm")
    private LocalDateTime createDate;
    private String procedureName;
    private String procedureNo;
    private String procedureStatus;
    private BigDecimal humanMinutes;
    private BigDecimal machineMinutes;
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd hh:mm")
    private LocalDateTime endTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planStartDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planEndDate;
}
