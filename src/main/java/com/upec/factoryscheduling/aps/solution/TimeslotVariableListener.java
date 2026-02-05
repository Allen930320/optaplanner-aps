package com.upec.factoryscheduling.aps.solution;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.domain.variable.VariableListener;
import org.optaplanner.core.api.score.director.ScoreDirector;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;


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

            // 2. 更新开始和结束时间
            updateTimeslotTime(scoreDirector, timeslot, newMaintenance);
        } else {
            // 如果取消分配,清空时间
            clearTimeslotTime(scoreDirector, timeslot);
        }
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体添加前不需要特殊处理
        if (timeslot.getMaintenance() != null) {
        }
    }

    @Override
    public void afterEntityAdded(ScoreDirector<FactorySchedulingSolution> scoreDirector, Timeslot timeslot) {
        // 实体添加后,如果已分配,需要分配资源并更新时间
        if (timeslot.getMaintenance() != null) {
            WorkCenterMaintenance maintenance = timeslot.getMaintenance();
            // 分配容量
            allocateCapacity(maintenance, timeslot.getDuration());
            // 更新时间
            updateTimeslotTime(scoreDirector, timeslot, maintenance);

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
     * 更新时间槽的开始和结束时间
     * <p>
     * 逻辑:
     * - 开始时间: 使用工作中心当天的开始时间,如果未设置则使用默认值09:00
     * - 结束时间: 使用工作中心当天的结束时间,如果未设置则使用默认值18:00
     * <p>
     * 注意: 根据需求,不需要精确计算每个时间槽的具体执行时间,
     * 只需要知道在哪一天的工作时间段内执行即可
     */
    private void updateTimeslotTime(ScoreDirector<FactorySchedulingSolution> scoreDirector,
                                    Timeslot timeslot,
                                    WorkCenterMaintenance maintenance) {
        // 通知ScoreDirector即将变更startTime
        scoreDirector.beforeVariableChanged(timeslot, "startTime");

        // 获取或使用默认开始时间
        LocalDateTime startTime = maintenance.getStartTime();
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
