package com.upec.factoryscheduling.mes.controller;

import com.upec.factoryscheduling.common.utils.ApiResponse;
import com.upec.factoryscheduling.mes.dto.OrderTaskDTO;
import com.upec.factoryscheduling.mes.dto.ProcedureQueryDTO;
import com.upec.factoryscheduling.mes.response.OrderTaskQueryResponse;
import com.upec.factoryscheduling.mes.service.MesJjOrderTaskService;
import com.upec.factoryscheduling.mes.service.MesJjProcedureService;
import com.upec.factoryscheduling.mes.service.MesOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mesOrders")
@CrossOrigin
public class MesOrderController {

    @Autowired
    private MesOrderService mesOrderService;

    @Autowired
    private MesJjOrderTaskService mesJjOrderTaskService;

    @Autowired
    private MesJjProcedureService mesJjProcedureService;

    @PostMapping("/syncData")
    public ApiResponse<Void> syncData(@RequestBody List<String> orderNos) {
//        mesOrderService.mergePlannerData(orderNos);
        return ApiResponse.success();
    }

    /**
     * 根据条件分页查询订单任务数据（新接口，支持关联查询和分页）
     */
    @GetMapping("orderTasks/page")
    public ApiResponse<Page<OrderTaskDTO>> queryOrderTaskForPage(
            @RequestParam(required = false) String orderName,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String contractNum,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) List<String> statusList,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<OrderTaskDTO> result = mesJjOrderTaskService.queryOrderTaskForPage(
                orderName, orderNo, contractNum, startTime, endTime, statusList, pageNum, pageSize);
        return ApiResponse.success(result);
    }

    @GetMapping("procedure/page")
    public ApiResponse<Page<ProcedureQueryDTO>> queryProcedures(
            @RequestParam(required = false) String orderName,
            @RequestParam(required = false) String taskNo,
            @RequestParam(required = false) String contractNum,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) List<String> statusList,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(mesJjProcedureService.queryProcedures(orderName, taskNo, contractNum, productCode,
                statusList, startDate, endDate, pageNum, pageSize));
    }
}
