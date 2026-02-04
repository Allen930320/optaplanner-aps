package com.upec.factoryscheduling.aps.repository.query.impl;

import com.upec.factoryscheduling.aps.dto.TaskTimeslotDTO;
import com.upec.factoryscheduling.aps.repository.query.OrderTaskQuery;
import com.upec.factoryscheduling.common.utils.JdbcTemplatePagination;
import com.upec.factoryscheduling.common.utils.UserContext;
import com.upec.factoryscheduling.mes.dto.OrderTaskDTO;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

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


    @Override
    public Page<TaskTimeslotDTO> queryTaskWithTimeslotByUser(String productName,
                                                             String productCode,
                                                             String taskNo,
                                                             String contractNum,
                                                             String startTime,
                                                             String endTime,
                                                             Integer pageNum,
                                                             Integer pageSize) {
        String userName = UserContext.getCurrentUsername();
        String querySQL = "select distinct t2.order_no, t2.task_no, t3.product_name, t3.product_code, t4.contract_num " +
                "from aps_procedure t2 " +
                "inner join mes_jj_procedure t5 on t5.seq = t2.id " +
                "left join mes_jj_procedure_joiner t1 on t1.procedure_seq = t2.id " +
                "left join mes_jj_order_product_info t3 on t3.orderno = t2.order_no " +
                "left join aps_orders t4 on t4.order_no = t2.order_no " +
                "where ((t2.status in ('执行中', '待执行', '初始导入') and t1.product_user = '" + userName + "') or " +
                "      (t2.status in ('待质检','质检中') and t5.quality_user||',' like '%" + userName + ",%'))";
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

    @Override
    public Page<OrderTaskDTO> queryApsTaskPage(String orderName,
                                               String orderNo,
                                               String contractNum,
                                               String startTime,
                                               String endTime,
                                               List<String> statusList,
                                               Integer pageNum,
                                               Integer pageSize) {

        String querySQL = " select t1.contractnum, " +
                "       t4.task_no, " +
                "       t4.order_no, " +
                "       t2.route_seq, " +
                "       t2.plan_quantity, " +
                "       t2.task_status, " +
                "       t2.createuser, " +
                "       t2.createdate, " +
                "       t4.plan_start_date, " +
                "       t4.plan_end_date, " +
                "       lockeduser, " +
                "       lockeddate, " +
                "       before_taskstatus as before_task_status, " +
                "       t4.locked_remark, " +
                "       mark, " +
                "       t3.product_code, " +
                "       t3.product_name " +
                " from mes_jj_order t1 " +
                "         inner join mes_jj_order_task t2 on t1.orderno = t2.orderno and t2.route_seq is not null " +
                "         inner join mes_jj_order_product_info t3 on t2.orderno = t3.orderno " +
                "         inner join aps_task t4 on t4.task_no = t2.taskno " +
                " where  t2.task_status <> '生产完成' ";
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
            querySQL = querySQL + " and t2.createdate between '" + startTime + "' and '" + endTime + "' ";
        }
        if (!CollectionUtils.isEmpty(statusList)) {
            String status = statusList.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            querySQL = querySQL + " and t2.task_status in (" + status + ")";
        }
        querySQL = querySQL + " order by t4.order_no desc, to_number(substr(t4.task_no, instr(t4.task_no, '_') + 1," +
                "length(t4.task_no))) ";
        return super.queryForPage(querySQL, new BeanPropertyRowMapper<>(OrderTaskDTO.class), pageNum, pageSize);
    }


}
