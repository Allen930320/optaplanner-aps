package com.upec.factoryscheduling.aps.repository.query.impl;

import com.upec.factoryscheduling.aps.dto.RouteProcedureQueryDTO;
import com.upec.factoryscheduling.aps.repository.query.RouteProcedureQuery;
import com.upec.factoryscheduling.common.utils.JdbcTemplatePagination;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class RouteProcedureQueryImpl extends JdbcTemplatePagination implements RouteProcedureQuery {


    @Override
    public Page<RouteProcedureQueryDTO> queryRouteProcedurePage(String productName,
                                                                String productCode,
                                                                String orderNo,
                                                                String taskNo,
                                                                String contractNum,
                                                                Integer pageNum,
                                                                Integer pageSize) {
        String querySQL = "select distinct t1.seq            as id, " +
                "       t1.route_seq, " +
                "       t1.procedureno    as procedure_no, " +
                "       t1.procedure_code, " +
                "       t1.procedure_name, " +
                "       t1.next_procedureno, " +
                "       t1.procedure_type, " +
                "       t1.procedure_content, " +
                "       t1.workcenter_seq as work_center_id, " +
                "       t1.machine_hours, " +
                "       t1.human_hours, " +
                "       t1.program_name, " +
                "       t1.dutyuser       as duty_user, " +
                "       t1.remark, " +
                "       t1.createuser     as create_user, " +
                "       t1.createdate     as create_date, " +
                "       t1.updateuser     as update_user, " +
                "       t1.updatedate     as update_date, " +
                "       t2.plan_days      as days, " +
                "       t3.product_code, " +
                "       t3.product_version, " +
                "       t3.route_code, " +
                "       t3.route_name, " +
                "       t3.route_version " +
                "from mes_jj_route_procedure t1 " +
                "inner join mes_jj_route_info t3 on t3.seq = t1.route_seq and route_name is not null " +
                "inner join mes_jj_procedure t4 on t4.route_seq = t1.route_seq " +
                "inner join mes_jj_order_task t5 on t5.taskno = t4.taskno " +
                "inner join mes_jj_order t6 on t6.orderno = t5.orderno " +
                "left join aps_route_procedure t2 on t1.seq = t2.id " +
                "where  t1.createdate is not null ";
        if (StringUtils.hasLength(orderNo)) {
            querySQL = querySQL + " and t5.orderno like '%" + orderNo + "%' ";
        }
        if (StringUtils.hasLength(taskNo)) {
            querySQL = querySQL + " and  t5.taskno like '%" + taskNo + "%' ";
        }
        if (StringUtils.hasLength(contractNum)) {
            querySQL = querySQL + " and t6.contractnum like '%" + contractNum + "%'";
        }
        if (StringUtils.hasLength(productCode)) {
            querySQL = querySQL + "  and t3.route_code like '%" + productCode + "%'";
        }
        if (StringUtils.hasLength(productName)) {
            querySQL = querySQL + " and t3.route_name like '%" + productName + "%' ";
        }
        querySQL = querySQL + "order by create_date desc ,t1.route_seq,to_number(procedure_no)";
        return super.queryForPage(querySQL, new BeanPropertyRowMapper<>(RouteProcedureQueryDTO.class), pageNum, pageSize);
    }
}
