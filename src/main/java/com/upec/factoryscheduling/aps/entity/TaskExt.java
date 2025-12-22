package com.upec.factoryscheduling.aps.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TaskExt extends Task {

    private List<String> outsourcingNo;
    private List<String> outsourcingId;
    private List<Procedure> procedures;
}
