package com.upec.factoryscheduling.aps.solution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import lombok.Getter;
import lombok.Setter;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.solver.SolverStatus;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@PlanningSolution
public class FactorySchedulingSolution implements  Serializable {
    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private Long problemId;

    @Getter
    @PlanningEntityCollectionProperty
    private List<Timeslot> timeslots;

    @JsonIgnore
    @Getter
    @ValueRangeProvider(id = "maintenanceRange")
    @ProblemFactCollectionProperty
    private List<WorkCenterMaintenance> maintenances;

    @PlanningScore
    private volatile HardMediumSoftScore score;

    private volatile SolverStatus solverStatus;
    
    /**
     * 获取规划分数
     */
    public synchronized HardMediumSoftScore getScore() {
        return score;
    }
    
    /**
     * 设置规划分数
     */
    public synchronized void setScore(HardMediumSoftScore score) {
        this.score = score;
    }
    
    /**
     * 获取求解器状态
     */
    public synchronized SolverStatus getSolverStatus() {
        return solverStatus;
    }
    
    /**
     * 设置求解器状态
     */
    public synchronized void setSolverStatus(SolverStatus solverStatus) {
        this.solverStatus = solverStatus;
    }


    public FactorySchedulingSolution() {
        this.timeslots = new CopyOnWriteArrayList<>();
        this.maintenances = new CopyOnWriteArrayList<>();
    }


    public FactorySchedulingSolution(List<Timeslot> timeslots,
                                     List<WorkCenterMaintenance> maintenances) {
        this.timeslots = timeslots != null ? new CopyOnWriteArrayList<>(timeslots) : new CopyOnWriteArrayList<>();
        this.maintenances = maintenances != null ? new CopyOnWriteArrayList<>(maintenances) : new CopyOnWriteArrayList<>();
    }


    public synchronized void setTimeslots(List<Timeslot> timeslots) {
        this.timeslots = timeslots != null ? new CopyOnWriteArrayList<>(timeslots) : new CopyOnWriteArrayList<>();
    }


    public synchronized void setMaintenances(List<WorkCenterMaintenance> maintenances) {
        this.maintenances = maintenances != null ? new CopyOnWriteArrayList<>(maintenances) : new CopyOnWriteArrayList<>();
    }


    public synchronized void addTimeslot(Timeslot timeslot) {
        if (timeslot != null) {
            this.timeslots.add(timeslot);
        }
    }

    public synchronized void addMaintenance(WorkCenterMaintenance maintenance) {
        if (maintenance != null) {
            this.maintenances.add(maintenance);
        }
    }

    public synchronized boolean removeTimeslot(Timeslot timeslot) {
        return timeslot != null && this.timeslots.remove(timeslot);
    }

    public synchronized boolean removeMaintenance(WorkCenterMaintenance maintenance) {
        return maintenance != null && this.maintenances.remove(maintenance);
    }


}
