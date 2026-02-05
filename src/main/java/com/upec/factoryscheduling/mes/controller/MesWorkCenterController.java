package com.upec.factoryscheduling.mes.controller;

import com.upec.factoryscheduling.common.utils.ApiResponse;
import com.upec.factoryscheduling.mes.entity.MesBaseWorkCenter;
import com.upec.factoryscheduling.mes.service.MesBaseWorkCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mes_work_center")
@CrossOrigin
public class MesWorkCenterController {

    private MesBaseWorkCenterService mesBaseWorkCenterService;

    @Autowired
    public void setMesBaseWorkCenterService(MesBaseWorkCenterService mesBaseWorkCenterService) {
        this.mesBaseWorkCenterService = mesBaseWorkCenterService;
    }

    @GetMapping("/list")
    public ApiResponse<List<MesBaseWorkCenter>> queryAllWorkCenter() {
        List<MesBaseWorkCenter> mesBaseWorkCenters = mesBaseWorkCenterService.findAllByFactorySeq("2");
        return ApiResponse.success(mesBaseWorkCenters);
    }
}
