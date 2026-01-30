package com.upec.factoryscheduling.aps.dto;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TaskTimeslotDTO implements Serializable {

    private String orderNo;
    private String taskNo;
    private String contractNum;
    private String productCode;
    private String productName;
    private List<Timeslot> timeslots;
}
