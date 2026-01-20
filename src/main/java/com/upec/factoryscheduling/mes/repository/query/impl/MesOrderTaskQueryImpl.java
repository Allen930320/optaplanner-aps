package com.upec.factoryscheduling.mes.repository.query.impl;

import com.upec.factoryscheduling.common.utils.JdbcTemplatePagination;
import com.upec.factoryscheduling.mes.dto.OrderTaskDTO;
import com.upec.factoryscheduling.mes.repository.query.MesOrderTaskQuery;
import org.intellij.lang.annotations.Language;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MesOrderTaskQueryImpl extends JdbcTemplatePagination implements MesOrderTaskQuery {


    @Override
    public Page<OrderTaskDTO> queryOrderTaskForPage(String orderName,
                                                    String orderNo,
                                                    String contractNum,
                                                    String startTime,
                                                    String endTime,
                                                    List<String> statusList,
                                                    Integer pageNum,
                                                    Integer pageSize) {

        @Language("sql") String querySQL = " select t1.contractnum, " +
                "       t2.taskno, " +
                "       t2.orderno, " +
                "       t2.route_seq, " +
                "       t2.plan_quantity , " +
                "       t2.task_status, " +
                "       t2.fact_startdate as fact_start_date, " +
                "       t2.fact_enddate as fact_end_date, " +
                "       t2.createuser, " +
                "       t2.createdate, " +
                "       t2.plan_startdate as plan_start_date, " +
                "       t2.plan_enddate as plan_end_date, " +
                "       t2.old_taskno as old_task_no, " +
                "       lockeduser, " +
                "       lockeddate, " +
                "       before_taskstatus as before_task_status, " +
                "       lockedremark, " +
                "       mark, " +
                "       t3.product_code, " +
                "       t3.product_name " +
                " from mes_jj_order t1 " +
                " inner join mes_jj_order_task t2 on t1.orderno = t2.orderno and t2.route_seq is not null " +
                " inner join mes_jj_order_product_info t3 on t2.orderno = t3.orderno " +
                " left join aps_task t4 on t4.task_no=t2.taskno " +
                " where t4.task_no is null ";
        if (StringUtils.hasLength(orderName)) {
            querySQL = querySQL + " and t3.product_name like '%" + orderName + "%'";
        }
        if (StringUtils.hasLength(orderNo)) {
            querySQL = querySQL + " and t2.orderno  like  '%" + orderNo + "%'";
        }
        if (StringUtils.hasLength(contractNum)) {
            querySQL = querySQL + " and t2.contractnum like '%" + contractNum + "%'";
        }
        if (StringUtils.hasLength(startTime) && StringUtils.hasLength(endTime)) {
            querySQL = querySQL + " and t2.createdate between " + startTime + " and " + endTime;
        }
        if (!CollectionUtils.isEmpty(statusList)) {
            String status = statusList.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            querySQL = querySQL + " and t2.task_status in (" + status + ")";
        }
        querySQL = querySQL + " order by t2.orderno desc, to_number(substr(t2.taskno, instr(t2.taskno, '_') + 1," +
                " length(t2.taskno))) ";
        return super.queryForPage(
                querySQL,
                new BeanPropertyRowMapper<>(OrderTaskDTO.class),
                pageNum,
                pageSize
        );
    }
}
