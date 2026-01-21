package com.upec.factoryscheduling.mes.service;

import com.google.common.collect.Lists;
import com.upec.factoryscheduling.aps.entity.Order;
import com.upec.factoryscheduling.aps.entity.Task;
import com.upec.factoryscheduling.aps.service.OrderService;
import com.upec.factoryscheduling.aps.service.OrderTaskService;
import com.upec.factoryscheduling.common.utils.DateUtils;
import com.upec.factoryscheduling.mes.entity.MesOrder;
import com.upec.factoryscheduling.mes.entity.MesOrderTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DataSynchronizationService {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("oracleTemplate")
    private void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    private OrderService orderService;

    @Autowired
    private void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    private OrderTaskService orderTaskService;

    @Autowired
    private void setOrderTaskService(OrderTaskService orderTaskService) {
        this.orderTaskService = orderTaskService;
    }

    private MesOrderService mesOrderService;

    @Autowired
    public void setMesOrderService(MesOrderService mesOrderService) {
        this.mesOrderService = mesOrderService;
    }

    public List<Order> syncMesOrders() {
        LocalDateTime now = LocalDateTime.of(2025,1,1,0,0);
        String start = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String sql = " select t1.ORDERNO, " +
                "       t1.PLAN_QUANTITY, " +
                "       t1.ORDER_STATUS, " +
                "       t1.ERP_STATUS, " +
                "       t1.CONTRACTNUM as CONTRACT_NUM, " +
                "       t1.PLAN_STARTDATE as PLAN_START_DATE, " +
                "       t1.PLAN_ENDDATE as PLAN_END_DATE,  " +
                "       t3.PRODUCT_CODE, " +
                "       t3.PRODUCT_NAME, " +
                "       t1.CREATEDATE as CREATE_DATE, " +
                "       t1.FACT_STARTDATE as FACT_START_DATE, " +
                "       t1.FACT_ENDDATE as FACT_END_DATE from MES_JJ_ORDER t1 " +
                " inner join MES_JJ_ORDER_PRODUCT_INFO t3 on t1.ORDERNO=t3.ORDERNO " +
                " where  t1.ORDERNO like '00400%' " +
                " and t1.CREATEDATE >= '" + start + "' and t1.ORDERNO not in (select t2.ORDER_NO from APS_ORDERS t2 " +
                " where t2.CREATE_DATE >= TO_DATE('" + start + "','YYYY-MM-DD HH24:MI:SS'))";
        List<Order> orders = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Order.class));
       return orderService.saveAll(orders);
    }

    public void syncUpdateMesOrders() {
        String sql = " SELECT T1.* FROM MES_JJ_ORDER T1 " +
                " INNER JOIN APS_ORDERS T2 ON T2.ORDER_NO=T1.ORDERNO " +
                " WHERE T1.ORDER_STATUS!=T2.ORDER_STATUS " +
                "   AND (TO_NUMBER(T1.PLAN_QUANTITY)= T2.PLAN_QUANTITY " +
                "   OR TO_DATE(T1.PLAN_ENDDATE, 'YYYY-MM-DD') != T2.PLAN_END_DATE " +
                "   OR TO_DATE(T1.PLAN_STARTDATE, 'YYYY-MM-DD') != T2.PLAN_START_DATE " +
                "   OR TO_DATE(T1.FACT_STARTDATE, 'YYYY-MM-DD HH24:MI:SS')!= T2.FACT_START_DATE " +
                "   OR TO_DATE(T1.FACT_ENDDATE, 'YYYY-MM-DD HH24:MI:SS') != T2.FACT_END_DATE )" +
                " AND T1.CREATEDATE >= '2025-01-01 00:00:00' AND T1.ORDERNO LIKE '00400%' ";
        List<MesOrder> mesJjOrders = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(MesOrder.class));
        for (MesOrder mesJjOrder : mesJjOrders) {
            Order order = orderService.getOrderById(mesJjOrder.getOrderNo()).orElse(null);
            if (order != null) {
                order.setOrderStatus(mesJjOrder.getOrderStatus());
                order.setErpStatus(mesJjOrder.getErpStatus());
                order.setContractNum(mesJjOrder.getContractNum());
                order.setPlanStartDate(DateUtils.parseLocalDate(mesJjOrder.getPlanStartDate()));
                order.setPlanEndDate(DateUtils.parseLocalDate(mesJjOrder.getPlanEndDate()));
                order.setFactStartDate(DateUtils.parseDateTime(mesJjOrder.getFactStartDate()));
                order.setFactEndDate(DateUtils.parseDateTime(mesJjOrder.getFactEndDate()));
                order.setPlanQuantity(Integer.valueOf(mesJjOrder.getPlanQuantity()));
                orderService.save(order);
            }
        }
    }

    public void syncUpdateTask() {
        String sql = " SELECT T1.* FROM MES_JJ_ORDER_TASK T1 " +
                " INNER JOIN APS_TASK T2 ON T1.TASKNO = T2.TASK_NO " +
                " WHERE T1.TASK_STATUS != T2.STATUS " +
                "   AND ( TO_NUMBER(t1.PLAN_QUANTITY)= t2.PLANQUANTITY " +
                "   OR TO_DATE(T1.PLAN_ENDDATE, 'YYYY-MM-DD') != T2.PLAN_END_DATE " +
                "   OR TO_DATE(T1.PLAN_STARTDATE, 'YYYY-MM-DD') != T2.PLAN_START_DATE " +
                "   OR TO_DATE(T1.FACT_STARTDATE, 'YYYY-MM-DD HH24:MI:SS')!= T2.FACT_START_DATE " +
                "   OR TO_DATE(T1.FACT_ENDDATE, 'YYYY-MM-DD HH24:MI:SS') != T2.FACT_END_DATE )" +
                "  AND T1.CREATEDATE >= '2025-01-01 00:00:00' AND t1.ORDERNO like '00400%'  ";
        List<MesOrderTask> orderTasks = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(MesOrderTask.class));
        for (MesOrderTask orderTask : orderTasks) {
            Task task = orderTaskService.findById(orderTask.getTaskNo());
            if (task != null) {
                task.setStatus(orderTask.getTaskStatus());
                task.setRouteId(orderTask.getRouteSeq());
                task.setPlanStartDate(DateUtils.parseLocalDate(orderTask.getPlanStartDate()));
                task.setPlanEndDate(DateUtils.parseLocalDate(orderTask.getPlanEndDate()));
                task.setFactStartDate(DateUtils.parseDateTime(orderTask.getFactStartDate()));
                task.setFactEndDate(DateUtils.parseDateTime(orderTask.getFactEndDate()));
                task.setLockedRemark(orderTask.getLockedRemark());
                task.setPlanQuantity(Integer.valueOf(orderTask.getPlanQuantity()));
                orderTaskService.save(task);
            }
        }
    }

    public void syncUpdateProcedure() {

    }

    @Scheduled(cron = "0 57 * * * *")
    public void addOrderAndTaskAndProcedure(){
        List<Order> orders = syncMesOrders();
        Lists.partition(orders, 100).forEach(list -> mesOrderService.mergePlannerData(list));
    }
}
