package com.upec.factoryscheduling.aps.solution;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import java.util.List;

public class MachineVariableListener implements VariableListener<Timeslot, List<WorkCenter>> {
    @Override
    public void beforeVariableChanged(ScoreDirector<Timeslot> scoreDirector, List<WorkCenter> machines) {

    }

    @Override
    public void afterVariableChanged(ScoreDirector<Timeslot> scoreDirector, List<WorkCenter> machines) {

    }

    @Override
    public void beforeEntityAdded(ScoreDirector<Timeslot> scoreDirector, List<WorkCenter> machines) {

    }

    @Override
    public void afterEntityAdded(ScoreDirector<Timeslot> scoreDirector, List<WorkCenter> machines) {

    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Timeslot> scoreDirector, List<WorkCenter> machines) {

    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Timeslot> scoreDirector, List<WorkCenter> machines) {

    }
}
