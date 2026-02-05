package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import com.upec.factoryscheduling.aps.repository.WorkCenterMaintenanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class WorkCenterMaintenanceService {

    private WorkCenterMaintenanceRepository repository;

    @Autowired
    public void setRepository(WorkCenterMaintenanceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void updateWorkCenterMaintenance(WorkCenter workCenter, int capacity, String status) {
        LocalDate today = LocalDate.now();
        List<WorkCenterMaintenance> workCenterMaintenances = repository.findAllByWorkCenterAndCalendarDateGreaterThanEqual(workCenter, today);
        for (WorkCenterMaintenance workCenterMaintenance : workCenterMaintenances) {
            if (capacity != workCenterMaintenance.getCapacity()) {
                workCenterMaintenance.setCapacity(capacity);
            }
            if (!Objects.equals(status, workCenterMaintenance.getStatus())) {
                workCenterMaintenance.setStatus(status);
            }
            workCenterMaintenance.setStatus(status);
        }
        repository.saveAll(workCenterMaintenances);
    }

    public void deleteWorkCenterMaintenance(WorkCenter workCenter) {
        List<WorkCenterMaintenance> list = repository.findAllByWorkCenterAndCalendarDateGreaterThanEqual(workCenter, LocalDate.now());
        if (!CollectionUtils.isEmpty(list)) {
            repository.deleteAll(list);
        }
    }

    public List<WorkCenterMaintenance> findAllByWorkCenterCodeAndLocalDateBetween(String workCenterCode, LocalDate startDate, LocalDate endDate){
      return repository.findAllByWorkCenter_WorkCenterCodeAndCalendarDateBetween(workCenterCode, startDate, endDate);
    }

    public List<WorkCenterMaintenance> findAllByWorkCenterCodeAndLocalDateBetween(List<WorkCenter> workCenters, LocalDate startDate, LocalDate endDate){
        return repository.findAllByWorkCenterIsInAndCalendarDateBetween(workCenters,startDate,endDate);
    }

    public void batchCreateWorkCenterMaintenance(List<WorkCenterMaintenance> maintenances) {
        repository.saveAll(maintenances);
    }


    public LocalDate findFirstByCalendarDateMax(){
        return repository.findFirstByCalendarDateMax();
    }

    public void updateWorkCenterMaintenance(WorkCenterMaintenance maintenance) {
        WorkCenterMaintenance updatedMaintenance = repository.findById(maintenance.getId()).orElse(null);
        if (updatedMaintenance != null) {
            updatedMaintenance.setCapacity(maintenance.getCapacity());
            updatedMaintenance.setStatus(maintenance.getStatus());
            repository.save(updatedMaintenance);
        }
    }

}
