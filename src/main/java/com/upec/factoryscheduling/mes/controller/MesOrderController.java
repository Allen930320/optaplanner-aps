package com.upec.factoryscheduling.mes.controller;

import com.upec.factoryscheduling.mes.service.MesOrderService;
import com.upec.factoryscheduling.mes.service.MesJjOrderTaskService;
import com.upec.factoryscheduling.utils.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import com.upec.factoryscheduling.mes.response.OrderTaskQueryResponse;

@RestController
@RequestMapping("/api/mesOrders")
@CrossOrigin
public class MesOrderController {

    @Autowired
    private MesOrderService mesOrderService;
    
    @Autowired
    private MesJjOrderTaskService mesJjOrderTaskService;

    @PostMapping("/syncData")
    public ApiResponse<Void> syncData(@RequestBody List<String> orderNos) {
        mesOrderService.mergePlannerData(orderNos);
        return ApiResponse.success();
    }

    /**
     * 根据条件分页查询订单任务数据（新接口，支持关联查询和分页）
     */
    @GetMapping("orderTasks/page")
    public ApiResponse<Page<OrderTaskQueryResponse>> queryOrderTasksWithPagination(
            @RequestParam(required = false) String orderName,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) List<String> statusList,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        
        Page<OrderTaskQueryResponse> result = mesJjOrderTaskService.findOrderTasksByConditionsWithPagination(
                orderName, startTime, endTime, statusList, pageNum, pageSize);
        return ApiResponse.success(result);
    }
}
