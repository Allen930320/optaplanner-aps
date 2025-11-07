package com.upec.factoryscheduling.aps.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.lookup.PlanningId;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "work_center_maintenances")
@Data
@Getter
@Setter
public class WorkCenterMaintenance {

    @Id
    @PlanningId
    private String id;

    @OneToOne(fetch = FetchType.EAGER)
    private WorkCenter workCenter;

    private int year;

    private LocalDate date;

    private int capacity;

    private String status;

    private String description;

    private LocalTime startTime;

    private LocalTime endTime;

    private int usageTime;

    public WorkCenterMaintenance() {
    }

    public WorkCenterMaintenance(WorkCenter workCenter, LocalDate date, int capacity, String description) {
        this.workCenter = workCenter;
        this.date = date;
        this.capacity = capacity;
        this.description = description;
    }

}
