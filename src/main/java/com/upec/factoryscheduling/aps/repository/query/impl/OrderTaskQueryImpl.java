package com.upec.factoryscheduling.aps.repository.query.impl;

import com.upec.factoryscheduling.aps.dto.TaskTimeslotDTO;
import com.upec.factoryscheduling.aps.repository.query.OrderTaskQuery;
import com.upec.factoryscheduling.common.utils.JdbcTemplatePagination;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderTaskQueryImpl extends JdbcTemplatePagination implements OrderTaskQuery {

    @Override
    public Page<TaskTimeslotDTO> queryTaskWithTimeslot(String productName,
                                                       String productCode,
                                                       String taskNo,
                                                       String contractNum,
                                                       String startTime,
                                                       String endTime,
                                                       Integer pageNum,
                                                       Integer pageSize) {
        String querySQL = " select distinct t2.order_no, t2.task_no, t3.product_name, t3.product_code,t4.contract_num" +
                " from  aps_procedure t2  " +
                " left join mes_jj_order_product_info t3 on t3.orderno = t2.order_no  " +
                " left join aps_orders t4 on t4.order_no=t2.order_no  " +
                " where 1=1 ";
        if (StringUtils.hasLength(productName)) {
            querySQL = querySQL + " and t3.product_name like '%" + productName + "%' ";
        }
        if (StringUtils.hasLength(productCode)) {
            querySQL = querySQL + " and t3.product_code like '%" + productCode + "%' ";
        }
        if (StringUtils.hasLength(taskNo)) {
            querySQL = querySQL + " and t2.task_no like '%" + taskNo + "%' ";
        }
        if (StringUtils.hasLength(contractNum)) {
            querySQL = querySQL + " and t4.contract_num like '%" + contractNum + "%' ";
        }
        if (StringUtils.hasLength(startTime) && StringUtils.hasLength(endTime)) {
            querySQL = querySQL + " and t4.create_date  between '" + startTime + "' and '" + endTime + "' ";
        }
        querySQL = querySQL + " order by t2.order_no desc, to_number(substr(t2.task_no, instr(t2.task_no, '_') + 1, " +
                " length(t2.task_no))) ";
        return super.queryForPage(querySQL, new BeanPropertyRowMapper<>(TaskTimeslotDTO.class), pageNum, pageSize);
    }

}
