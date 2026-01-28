package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.dto.TaskTimeslotDTO;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.aps.service.TimeslotService;
import com.upec.factoryscheduling.aps.solution.FactorySchedulingSolution;
import com.upec.factoryscheduling.common.utils.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/timeslot")
@Slf4j
@CrossOrigin
public class TimeslotController {

    private TimeslotService timeslotService;

    @Autowired
    public void setTimeslotService(TimeslotService timeslotService) {
        this.timeslotService = timeslotService;
    }

    @GetMapping("/list")
    public ApiResponse<FactorySchedulingSolution> queryTimeslot() {
        return ApiResponse.success(timeslotService.findAll());
    }

    @PostMapping("/create")
    public ApiResponse<Void> createTimeslot(
            @RequestParam("taskNo") String taskNo,
            @RequestParam("procedureId") String procedureId,
            @RequestParam(value = "time", defaultValue = "0.5") double time,
            @RequestParam(value = "slice", defaultValue = "0") int slice) {
        timeslotService.createTimeslot(procedureId, time, slice);
        return ApiResponse.success();
    }


    @GetMapping("/{taskNo}/list")
    public ApiResponse<List<Timeslot>> findAllTimeslotByTaskNoIn(@PathVariable("taskNo") String taskNo) {
        return ApiResponse.success(timeslotService.findAllByTaskIn(List.of(taskNo)));
    }


    @PostMapping("/{timeslotId}/split")
    public ApiResponse<Void> splitOutsourcingTimeslot(@PathVariable("timeslotId") String timeslotId,
                                                      @RequestParam("days") int days) {
        timeslotService.splitOutsourcingTimeslot(timeslotId, days);
        return ApiResponse.success();
    }


    @GetMapping("/page")
    public ApiResponse<Page<TaskTimeslotDTO>> queryTimeslots(@RequestParam(required = false) String productName,
                                                             @RequestParam(required = false) String productCode,
                                                             @RequestParam(required = false) String contractNum,
                                                             @RequestParam(required = false) String startTime,
                                                             @RequestParam(required = false) String endTime,
                                                             @RequestParam(required = false) String taskNo,
                                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<TaskTimeslotDTO> page = timeslotService.queryTimeslots(productName, productCode, taskNo, contractNum, startTime, endTime, pageNum, pageSize);
        return ApiResponse.success(page);
    }

    @GetMapping("/pageOfProductUser")
    public ApiResponse<Page<TaskTimeslotDTO>> queryTimeslotsByProductUser(@RequestParam(required = false) String productName,
                                                             @RequestParam(required = false) String productCode,
                                                             @RequestParam(required = false) String contractNum,
                                                             @RequestParam(required = false) String startTime,
                                                             @RequestParam(required = false) String endTime,
                                                             @RequestParam(required = false) String taskNo,
                                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                                             @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<TaskTimeslotDTO> page = timeslotService.queryTimeslotsByProductUser(productName, productCode, taskNo, contractNum, startTime,
                endTime, pageNum, pageSize);
        return ApiResponse.success(page);
    }

}
