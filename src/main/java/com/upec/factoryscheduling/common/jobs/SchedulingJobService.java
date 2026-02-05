package com.upec.factoryscheduling.common.jobs;

import com.upec.factoryscheduling.aps.service.WorkCenterMaintenanceService;
import com.upec.factoryscheduling.aps.service.WorkCenterService;
import com.upec.factoryscheduling.mes.service.MesBaseWorkCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Slf4j
public class SchedulingJobService {

    private MesBaseWorkCenterService mesBaseWorkCenterService;

    @Autowired
    private void setMesBaseWorkCenterService(MesBaseWorkCenterService mesBaseWorkCenterService) {
        this.mesBaseWorkCenterService = mesBaseWorkCenterService;
    }

    private WorkCenterService workCenterService;

    @Autowired
    private void setWorkCenterService(WorkCenterService workCenterService) {
        this.workCenterService = workCenterService;
    }

    private WorkCenterMaintenanceService workCenterMaintenanceService;

    @Autowired
    private void setWorkCenterMaintenanceService(WorkCenterMaintenanceService workCenterMaintenanceService) {
        this.workCenterMaintenanceService = workCenterMaintenanceService;
    }

    @Scheduled(cron = "0 0/5 * * * *")
    @Transactional
    public void syncWorkCenterData() {
        mesBaseWorkCenterService.syncWorkCenterData();
    }


    @Scheduled(cron = "0 0/7 * * * *")
    public void createWorkCenterMaintenance() {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = workCenterMaintenanceService.findFirstByCalendarDateMax();
        if (maxDate != null) {
            int diff = maxDate.getYear() - today.getYear();
            if (diff == 0) {
                LocalDate start = LocalDate.of(maxDate.getYear() + 1, 1, 1);
                LocalDate end = LocalDate.of(maxDate.getYear() + 1, 12, 31);
                workCenterService.autoCreateWorkCenterMaintenance(start, end);
            }
        } else {
            LocalDate start = LocalDate.of(today.getYear(), 1, 1);
            LocalDate end = LocalDate.of(today.getYear(), 12, 31);
            workCenterService.autoCreateWorkCenterMaintenance(start, end);
        }

    }
}
