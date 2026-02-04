package com.upec.factoryscheduling.aps.controller;

import com.upec.factoryscheduling.aps.entity.Order;
import com.upec.factoryscheduling.aps.entity.Task;
import com.upec.factoryscheduling.aps.service.OrderService;
import com.upec.factoryscheduling.aps.service.OrderTaskService;
import com.upec.factoryscheduling.common.utils.ApiResponse;
import com.upec.factoryscheduling.mes.dto.OrderTaskDTO;
import org.checkerframework.checker.formatter.qual.Format;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 订单控制器
 * <p>提供订单相关的REST API端点，支持订单的查询、创建和删除操作。</p>
 * <p>该控制器处理工厂生产订单的基础管理功能，为工厂调度系统提供订单数据。</p>
 */
@RestController  // 标记此类为REST控制器
@RequestMapping("/api/orders")  // 设置API基础路径
@CrossOrigin  // 允许跨域请求
public class OrderController {

    /**
     * 订单服务 - 提供订单相关的业务逻辑
     */
    private OrderService orderService;

    @Autowired
    private OrderTaskService orderTaskService;


    @Autowired
    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public ApiResponse<Order> getOrderById(@PathVariable String id) {  // 从URL路径中提取ID参数
        return orderService.getOrderById(id)
                .map(ApiResponse::success)  // 找到记录时返回成功响应
                .orElse(ApiResponse.error("未找到指定ID的订单"));  // 未找到记录时返回错误响应
    }


    @PostMapping
    public ApiResponse<List<Order>> createOrders(@RequestBody List<Order> orders) {  // 从请求体中提取订单列表
        return ApiResponse.success(orderService.createOrders(orders));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteOrder(@PathVariable String id) {
        orderService.deleteOrder(id);
        return ApiResponse.success();  // 返回成功响应
    }

    @GetMapping("/task/page")
    public ApiResponse<Page<OrderTaskDTO>> queryApsOrderTaskForPage(
            @RequestParam(required = false) String orderName,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String contractNum,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) List<String> statusList,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(orderTaskService.queryApsTaskPage(orderName,
                orderNo,
                contractNum,
                startTime,
                endTime,
                statusList,
                pageNum,
                pageSize));
    }


    @PostMapping("/task/setEndDate")
    public ApiResponse<Void> setPlanStartDate(@RequestParam String taskNo,
                                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate planStartDate,
                                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate planEndDate) {
        orderTaskService.setPlanStartDate(taskNo, planStartDate, planEndDate);
        return ApiResponse.success();
    }
}
