package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.dto.RouteProcedureQueryDTO;
import com.upec.factoryscheduling.aps.service.RouteProcedureService;
import com.upec.factoryscheduling.common.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/route-procedure")
@CrossOrigin
public class RouteProcedureController {

    private RouteProcedureService routeProcedureService;

    @Autowired
    private void setRouteProcedureService(RouteProcedureService routeProcedureService) {
        this.routeProcedureService = routeProcedureService;
    }

    @GetMapping("/queryPage")
    public ApiResponse<Page<RouteProcedureQueryDTO>> queryRouteProcedurePage(@RequestParam(required = false) String productName,
                                                                             @RequestParam(required = false) String productCode,
                                                                             @RequestParam(required = false) String orderNo,
                                                                             @RequestParam(required = false) String taskNo,
                                                                             @RequestParam(required = false) String contractNum,
                                                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                                                             @RequestParam(defaultValue = "20") Integer pageSize) {

        return ApiResponse.success(routeProcedureService.queryRouteProcedurePage(productName, productCode, orderNo, taskNo, contractNum, pageNum, pageSize));
    }
}
