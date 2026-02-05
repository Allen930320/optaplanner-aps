package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.service.WorkCenterService;
import com.upec.factoryscheduling.common.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work_center")
@CrossOrigin
public class MachineController {

    private final WorkCenterService workCenterService;


    @Autowired
    public MachineController(WorkCenterService workCenterService) {
        this.workCenterService = workCenterService;
    }

    @GetMapping("/{id}/work-center")
    public ApiResponse<WorkCenter> getMachineById(@PathVariable String id) {
        return workCenterService.getMachineById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("未找到指定ID的工作中心"));
    }

    @PostMapping
    public ApiResponse<List<WorkCenter>> createMachines(@RequestBody List<WorkCenter> machines) {
        return ApiResponse.success(workCenterService.create(machines));
    }

    @PutMapping("/{id}/work-center")
    public ApiResponse<WorkCenter> updateMachine(@PathVariable String id,
                                                 @RequestParam(required = false) int capacity,
                                                 @RequestParam(required = false) String status) {
        return ApiResponse.success(workCenterService.updateMachine(id, capacity, status));
    }

    @DeleteMapping("/{id}/work-center")
    public ApiResponse<Void> deleteMachine(@PathVariable String id) {
        workCenterService.deleteMachine(id);
        return ApiResponse.success();
    }

    @GetMapping("/page")
    public ApiResponse<Page<WorkCenter>> queryWorkCenterPage(@RequestParam(required = false) String name,
                                                             @RequestParam(required = false) String code,
                                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(workCenterService.queryWorkCenterPage(name, code, pageNum, pageSize));
    }

    @GetMapping("/list")
    public ApiResponse<List<WorkCenter>> queryWorkCenters(@RequestParam(required = false) String spell) {
        return ApiResponse.success(workCenterService.queryWorkCenter(spell));
    }
}
