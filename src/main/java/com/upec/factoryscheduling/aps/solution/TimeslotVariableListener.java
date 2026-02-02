package com.upec.factoryscheduling.aps.solution;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import lombok.extern.slf4j.Slf4j;
import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 时间槽变量监听器
 * 主要功能:
 * 1. 实时更新WorkCenterMaintenance的usageTime
 * 2. 动态计算Timeslot的startTime
 */
@Slf4j
public class TimeslotVariableListener implements VariableListener<FactorySchedulingSolution, Timeslot>, Serializable {

    private static final long serialVersionUID = 1L;

    // 默认工作时间
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);  // 09:00
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);   // 18:00

    @Override
    public void beforeVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 变量变更前,如果已分配,需要释放原有资源
        if (timeslot.getMaintenance() != null) {
            WorkCenterMaintenance oldMaintenance = timeslot.getMaintenance();
            releaseCapacity(oldMaintenance, timeslot.getDuration());
        }
    }

    @Override
    public void afterVariableChanged(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 变量变更后,如果已分配新值,需要分配资源并更新时间
        if (timeslot.getMaintenance() != null) {
            WorkCenterMaintenance newMaintenance = timeslot.getMaintenance();
            // 1. 分配容量资源
            allocateCapacity(newMaintenance, timeslot.getDuration());

            // 2. 更新开始时间
            updateTimeslotStartTime(scoreDirector, timeslot, newMaintenance);
        } else {
            // 如果取消分配,清空时间
            clearTimeslotTime(scoreDirector, timeslot);
        }
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体添加前不需要特殊处理
    }

    @Override
    public void afterEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体添加后,如果已分配,需要分配资源并更新时间
        if (timeslot.getMaintenance() != null) {
            WorkCenterMaintenance maintenance = timeslot.getMaintenance();

            // 分配容量
            allocateCapacity(maintenance, timeslot.getDuration());

            // 更新时间
            updateTimeslotStartTime(scoreDirector, timeslot, maintenance);
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体移除前,如果已分配,需要释放资源
        if (timeslot.getMaintenance() != null) {
            WorkCenterMaintenance maintenance = timeslot.getMaintenance();
            releaseCapacity(maintenance, timeslot.getDuration());
        }
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体移除后,清理可能的残留状态
    }

    /**
     * 分配容量资源
     * 使用WorkCenterMaintenance的线程安全方法
     */
    private void allocateCapacity(WorkCenterMaintenance maintenance, int duration) {
        if (duration > 0) {
            maintenance.addUsageTime(duration);
        }
    }

    /**
     * 释放容量资源
     * 使用WorkCenterMaintenance的线程安全方法
     */
    private void releaseCapacity(WorkCenterMaintenance maintenance, int duration) {
        if (duration > 0) {
            maintenance.subtractUsageTime(duration);
        }
    }

    /**
     * 更新时间槽的开始时间
     * 根据分配的工作中心来动态计算startTime
     */
    private void updateTimeslotStartTime(ScoreDirector<FactorySchedulingSolution> scoreDirector,
                                         Timeslot timeslot,
                                         WorkCenterMaintenance maintenance) {
        // 通知ScoreDirector即将变更startTime
        scoreDirector.beforeVariableChanged(timeslot, "startTime");

        // 计算开始时间
        LocalDateTime startTime;
        if (maintenance.getStartTime() != null) {
            // 使用工作中心维护计划中定义的开始时间
            startTime = maintenance.getStartTime();
        } else {
            // 使用默认开始时间 09:00
            startTime = LocalDateTime.of(maintenance.getDate(), DEFAULT_START_TIME);
        }

        timeslot.setStartTime(startTime);
        // 通知ScoreDirector startTime已变更
        scoreDirector.afterVariableChanged(timeslot, "startTime");
    }

    /**
     * 清空时间槽的时间信息
     */
    private void clearTimeslotTime(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        if (timeslot.getStartTime() != null && timeslot.getStartTime().toLocalTime().equals(DEFAULT_START_TIME)) {
            // 清空开始时间
            scoreDirector.beforeVariableChanged(timeslot, "startTime");
            timeslot.setStartTime(null);
            scoreDirector.afterVariableChanged(timeslot, "startTime");
        }
    }
}
