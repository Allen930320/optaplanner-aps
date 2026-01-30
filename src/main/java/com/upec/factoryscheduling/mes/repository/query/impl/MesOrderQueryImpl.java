package com.upec.factoryscheduling.mes.repository.query.impl;

import com.upec.factoryscheduling.aps.entity.Order;
import com.upec.factoryscheduling.common.utils.JdbcTemplatePagination;
import com.upec.factoryscheduling.mes.repository.query.MesOrderQuery;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MesOrderQueryImpl extends JdbcTemplatePagination implements MesOrderQuery {


    @Override
    public List<Order> queryOrderListNotInApsOrder(List<String> taskNos) {
        String querySQL = " select t1.orderno,  " +
                "       t1.plan_quantity,  " +
                "       t1.factory_code,  " +
                "       t1.prdmanager_seq as prd_manager_seq,  " +
                "       t1.order_type,  " +
                "       t1.erp_status,  " +
                "       t1.order_status,  " +
                "       t1.plan_startdate as plan_start_date,  " +
                "       t1.plan_enddate as plan_end_date,  " +
                "       t1.fact_startdate as fact_start_date,  " +
                "       t1.fact_enddate as fact_end_date,  " +
                "       t1.fact_quantity,  " +
                "       t1.createuser,  " +
                "       t1.createdate,  " +
                "       t1.contractnum,  " +
                "       t3.product_name,  " +
                "       t3.product_code  " +
                " from  mes_jj_order t1  " +
                " inner join mes_jj_order_task t2 on t1.orderno = t2.orderno  " +
                " inner join mes_jj_order_product_info t3 on t2.orderno = t3.orderno  " +
                " left  join aps_orders t4 on t4.order_no = t1.orderno " +
                " where t4.order_no is null ";
        if (!CollectionUtils.isEmpty(taskNos)) {
            String params = taskNos.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            querySQL = querySQL + " and t2.taskno in (" + params + " )";
        }

        return super.jdbcTemplate.query(querySQL, new BeanPropertyRowMapper<>(Order.class));
    }
}
