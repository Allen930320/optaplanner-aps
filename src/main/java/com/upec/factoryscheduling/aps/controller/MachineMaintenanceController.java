package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import com.upec.factoryscheduling.aps.service.WorkCenterMaintenanceService;
import com.upec.factoryscheduling.common.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/work_center_maintenance")
@CrossOrigin
public class MachineMaintenanceController {

    private WorkCenterMaintenanceService maintenanceService;

    @Autowired
    public void setMaintenanceService(WorkCenterMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }


    @GetMapping("/list")
    public ApiResponse<List<WorkCenterMaintenance>> findAllByWorkCenterCodeAndDate(@RequestParam("workCenterCode") String workCenterCode,
                                                                                   @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
                                                                                   @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return ApiResponse.success(maintenanceService.findAllByWorkCenterCodeAndLocalDateBetween(workCenterCode, startDate, endDate));
    }

    @PostMapping("/update")
    public ApiResponse<Void> updateWorkCenterMaintenance(@RequestBody WorkCenterMaintenance maintenance) {
        maintenanceService.updateWorkCenterMaintenance(maintenance);
        return ApiResponse.success();
    }
}
