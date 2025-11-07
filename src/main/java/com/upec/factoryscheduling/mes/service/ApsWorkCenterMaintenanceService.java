package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.mes.entity.ApsWorkCenterMaintenance;
import com.upec.factoryscheduling.mes.entity.MesBaseWorkCenter;
import com.upec.factoryscheduling.mes.repository.ApsWorkCenterMaintenanceRepository;
import com.upec.factoryscheduling.utils.RandomFun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApsWorkCenterMaintenanceService {

    private ApsWorkCenterMaintenanceRepository repository;

    @Autowired
    public void setRepository(ApsWorkCenterMaintenanceRepository repository) {
        this.repository = repository;
    }

    @Transactional("mysqlTransactionManager")
    public void createWorkCenterMaintenance(List<MesBaseWorkCenter> mesBaseWorkCenters) {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        for (MesBaseWorkCenter baseWorkCenter : mesBaseWorkCenters) {
            List<ApsWorkCenterMaintenance> workCenterMaintenances = new ArrayList<>();
            for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
                ApsWorkCenterMaintenance workCenterMaintenance = new ApsWorkCenterMaintenance();
                workCenterMaintenance.setId(RandomFun.getInstance().getRandom());
                workCenterMaintenance.setStatus("Active");
                workCenterMaintenance.setWorkCenterCode(baseWorkCenter.getWorkCenterCode());
                workCenterMaintenance.setLocalDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                workCenterMaintenance.setStartTime(date.atTime(9, 0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                workCenterMaintenance.setEndTime(date.atTime(17, 30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                workCenterMaintenance.setCapacity(480L);
                workCenterMaintenance.setDescription(baseWorkCenter.getDescription());
                workCenterMaintenances.add(workCenterMaintenance);
            }
            repository.saveAll(workCenterMaintenances);
        }
    }

    public List<ApsWorkCenterMaintenance> findAllByWorkCenterCodeIn(List<String> workCenterCodes) {
        return repository.findAllByWorkCenterCodeIn(workCenterCodes);
    }
}
