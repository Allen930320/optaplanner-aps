package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import com.upec.factoryscheduling.aps.repository.WorkCenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WorkCenterService {

    private WorkCenterRepository workCenterRepository;

    @Autowired
    private void setMachineRepository(WorkCenterRepository workCenterRepository) {
        this.workCenterRepository = workCenterRepository;
    }

    private WorkCenterMaintenanceService workCenterMaintenanceService;

    @Autowired
    private void setWorkCenterMaintenanceService(WorkCenterMaintenanceService workCenterMaintenanceService) {
        this.workCenterMaintenanceService = workCenterMaintenanceService;
    }

    public List<WorkCenter> getAllMachines() {
        return workCenterRepository.findAll();
    }


    public Optional<WorkCenter> getMachineById(String id) {
        return workCenterRepository.findById(id);
    }

    @Transactional
    public WorkCenter updateMachine(String id, int capacity, String status) {
        WorkCenter machine = workCenterRepository.findById(id).orElse(null);
        if (machine != null) {
            if (capacity != machine.getCapacity()) {
                machine.setCapacity(capacity);
            }
            if (status != null && !status.equals(machine.getStatus())) {
                machine.setStatus(status);
            }
            workCenterRepository.save(machine);
            workCenterMaintenanceService.updateWorkCenterMaintenance(machine, capacity, status);
        }
        return machine;
    }

    @Transactional
    public void deleteMachine(String id) {
        WorkCenter workCenter = workCenterRepository.findById(id).orElse(null);
        if (workCenter != null) {
            workCenterRepository.delete(workCenter);
            workCenterMaintenanceService.deleteWorkCenterMaintenance(workCenter);
        }
    }

    @Transactional
    public List<WorkCenter> create(List<WorkCenter> machines) {
        return workCenterRepository.saveAll(machines);
    }


    public WorkCenter findWorkCenterByCode(String workCode) {
        List<WorkCenter> workCenters = workCenterRepository.findAllByWorkCenterCode(workCode);
        if (!workCenters.isEmpty()) {
            return workCenters.get(0);
        }
        return null;
    }

    public List<WorkCenter> queryWorkCenter(String code) {
        if (!StringUtils.hasLength(code)) {
            return getAllMachines();
        }
        return workCenterRepository.findAllByNameLikeOrWorkCenterCodeLike(code, code);
    }


    public Page<WorkCenter> queryWorkCenterPage(String name, String code, Integer pageNum, Integer pageSize) {
        return workCenterRepository.queryWorkCenterPage(name, code, pageNum, pageSize);
    }


    public void autoCreateWorkCenterMaintenance(LocalDate startDate, LocalDate endDate) {
        List<WorkCenter> workCenters = getAllMachines();
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate);
        for (WorkCenter workCenter : workCenters) {
            List<WorkCenterMaintenance> maintenances = new ArrayList<>();
            for (int i = 0; i <= days; i++) {
                maintenances.add(new WorkCenterMaintenance(workCenter, startDate.plusDays(i)));
            }
            workCenterMaintenanceService.batchCreateWorkCenterMaintenance(maintenances);
        }
    }


}
