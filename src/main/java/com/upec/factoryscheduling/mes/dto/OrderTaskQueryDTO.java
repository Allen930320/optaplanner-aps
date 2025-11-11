package com.upec.factoryscheduling.mes.dto;

import java.util.Date;

public class OrderTaskQueryDTO {
    // 任务相关字段
    private String taskNo;        // 任务编号
    private String orderNo;       // 订单编号
    private String orderName;     // 订单名称
    private String routeSeq;      // 工艺路线
    private Integer planQuantity; // 计划数量
    private String taskStatus;    // 任务状态
    private Date planStartDate;   // 计划开始日期
    private Date planEndDate;     // 计划结束日期
    private Date factStartDate;   // 实际开始日期
    private Date factEndDate;     // 实际结束日期
    
    // 订单相关字段
    private Integer orderPlanQuantity; // 订单计划数量
    private String orderStatus;        // 订单状态
    private String contractNum;        // 合同编号
    
    // 构造函数
    public OrderTaskQueryDTO() {
    }
    
    // Getters and Setters
    public String getTaskNo() {
        return taskNo;
    }
    
    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }
    
    public String getOrderNo() {
        return orderNo;
    }
    
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    
    public String getOrderName() {
        return orderName;
    }
    
    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }
    
    public String getRouteSeq() {
        return routeSeq;
    }
    
    public void setRouteSeq(String routeSeq) {
        this.routeSeq = routeSeq;
    }
    
    public Integer getPlanQuantity() {
        return planQuantity;
    }
    
    public void setPlanQuantity(Integer planQuantity) {
        this.planQuantity = planQuantity;
    }
    
    public String getTaskStatus() {
        return taskStatus;
    }
    
    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }
    
    public Date getPlanStartDate() {
        return planStartDate;
    }
    
    public void setPlanStartDate(Date planStartDate) {
        this.planStartDate = planStartDate;
    }
    
    public Date getPlanEndDate() {
        return planEndDate;
    }
    
    public void setPlanEndDate(Date planEndDate) {
        this.planEndDate = planEndDate;
    }
    
    public Date getFactStartDate() {
        return factStartDate;
    }
    
    public void setFactStartDate(Date factStartDate) {
        this.factStartDate = factStartDate;
    }
    
    public Date getFactEndDate() {
        return factEndDate;
    }
    
    public void setFactEndDate(Date factEndDate) {
        this.factEndDate = factEndDate;
    }
    
    public Integer getOrderPlanQuantity() {
        return orderPlanQuantity;
    }
    
    public void setOrderPlanQuantity(Integer orderPlanQuantity) {
        this.orderPlanQuantity = orderPlanQuantity;
    }
    
    public String getOrderStatus() {
        return orderStatus;
    }
    
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public String getContractNum() {
        return contractNum;
    }
    
    public void setContractNum(String contractNum) {
        this.contractNum = contractNum;
    }
}