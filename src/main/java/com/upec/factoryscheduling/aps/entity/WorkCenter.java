package com.upec.factoryscheduling.aps.entity;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "aps_work_center")
@Getter
@Setter
public class WorkCenter {

    @Id
    @PlanningId
    private String id;
    @Column(name = "work_center_code")
    private String workCenterCode;
    private String name;
    private String status;

}
