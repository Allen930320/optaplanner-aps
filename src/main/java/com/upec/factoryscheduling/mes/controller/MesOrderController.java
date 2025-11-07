package com.upec.factoryscheduling.mes.controller;

import com.upec.factoryscheduling.aps.entity.Timeslot;
import com.upec.factoryscheduling.mes.entity.MesJjOrder;
import com.upec.factoryscheduling.mes.repository.MesOrderRepository;
import com.upec.factoryscheduling.mes.service.MesOrderService;
import com.upec.factoryscheduling.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mesOrders")
@CrossOrigin
public class MesOrderController {


    @Autowired
    private MesOrderService mesOrderService;

    @PostMapping("syncData")
    public ApiResponse<Void> syncData(@RequestBody List<String> orderNos) {
        mesOrderService.mergePlannerData(orderNos);
        return ApiResponse.success();
    }
}
