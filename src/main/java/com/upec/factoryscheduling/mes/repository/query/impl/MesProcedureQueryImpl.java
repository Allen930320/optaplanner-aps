package com.upec.factoryscheduling.mes.repository.query.impl;

import com.upec.factoryscheduling.common.utils.JdbcTemplatePagination;
import com.upec.factoryscheduling.mes.dto.ProcedureQueryDTO;
import com.upec.factoryscheduling.mes.entity.MesProcedure;
import com.upec.factoryscheduling.mes.repository.query.MesProcedureQuery;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MesProcedureQueryImpl extends JdbcTemplatePagination implements MesProcedureQuery {


    @Override
    public Page<ProcedureQueryDTO> procedureQueryDTOPage(String orderName,
                                                         String taskNo,
                                                         String contractNum,
                                                         String productCode,
                                                         List<String> statusList,
                                                         String startDate,
                                                         String endDate,
                                                         Integer pageNum,
                                                         Integer pageSize) {
        String querySQL = " select t1.contractnum, " +
                "       t2.taskno, " +
                "       t2.orderno, " +
                "       t2.task_status, " +
                "       p.create_date, " +
                "       t3.product_code, " +
                "       t3.product_name, " +
                "       p.id as procedure_id, " +
                "       p.procedure_type as procedureType ," +
                "       p.procedure_name, " +
                "       p.procedure_no, " +
                "       p.status as procedure_status, " +
                "       p.human_minutes, " +
                "       p.machine_minutes, " +
                "       p.start_time, " +
                "       p.end_time, " +
                "       t2.plan_startdate as plan_start_date, " +
                "       t2.plan_enddate as plan_end_date, " +
                "       wc.name as work_center_name " +
                " from mes_jj_order t1 " +
                "         inner join mes_jj_order_task t2 on t1.orderno = t2.orderno and t2.route_seq is not null " +
                "         inner join mes_jj_order_product_info t3 on t2.orderno = t3.orderno " +
                "         inner join aps_procedure p on p.task_no = t2.taskno " +
                "         inner join aps_task t4 on t4.task_no = t2.taskno " +
                "         left join aps_work_center wc on wc.id=p.work_center_id " +
                " where t4.task_no is not null ";
        if (StringUtils.hasLength(orderName)) {
            querySQL = querySQL + " and t3.product_name like   '%" + orderName + "%' ";
        }
        if (StringUtils.hasLength(taskNo)) {
            querySQL = querySQL + " and t2.taskno like '%" + taskNo + "%' ";
        }
        if (StringUtils.hasLength(contractNum)) {
            querySQL = querySQL + " and t1.contractnum like '%" + contractNum + "%' ";
        }
        if (StringUtils.hasLength(productCode)) {
            querySQL = querySQL + " and t3.product_code like '%" + productCode + "%' ";
        }
        if (!CollectionUtils.isEmpty(statusList)) {
            String status = statusList.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            querySQL = querySQL + " and p.status in (" + status + ")";
        }
        if (StringUtils.hasLength(startDate) && StringUtils.hasLength(endDate)) {
            querySQL = querySQL + " and p.create_date between '" + startDate + "' and '" + endDate + "' ";
        }
        querySQL += " order by t2.orderno desc, to_number(substr(t2.taskno, instr(t2.taskno, '_') + 1, length(t2.taskno))),to_number(p.procedure_no)";
        return super.queryForPage(
                querySQL,
                new BeanPropertyRowMapper<>(ProcedureQueryDTO.class),
                pageNum,
                pageSize
        );
    }


    public List<MesProcedure> queryMesProcedureNotInAps(List<String> taskNos) {
        String querySQL = " select t1.seq, " +
                "       t1.orderno, " +
                "       t1.taskno, " +
                "       t1.procedureno, " +
                "       t1.procedure_name, " +
                "       t1.next_procedureno as next_procedure_no, " +
                "       t1.procedure_type, " +
                "       t1.prdmanager_seq as prd_manager_seq, " +
                "       t1.workcenter_code, " +
                "       t1.prepare_hours, " +
                "       t1.machine_hours, " +
                "       t1.human_hours, " +
                "       t1.procedure_status, " +
                "       t1.fact_startdate as fact_start_date, " +
                "       t1.fact_enddate as fact_end_date, " +
                "       t1.quality_user, " +
                "       t1.rework_flag, " +
                "       t1.assist_processinstance as assist_process_instance, " +
                "       t1.assist_prdmanager_seq as assist_prd_manager_seq, " +
                "       t1.createuser, " +
                "       t1.createdate, " +
                "       t1.produre_hours, " +
                "       t1.erp_procedureno as erp_procedure_no, " +
                "       t1.plan_startdate as plan_start_date, " +
                "       t1.plan_enddate as plan_end_date, " +
                "       t1.unqualified_processinstance as unqualified_process_instance, " +
                "       t1.self_check_result, " +
                "       t1.self_check_remark, " +
                "       t1.route_seq, " +
                "       t1.updateuser, " +
                "       t1.updatedate, " +
                "       t1.makednumber, " +
                "       t1.quickprocessinstance as quick_process_instance " +
                " from mes_jj_procedure t1 " +
                " left join aps_procedure t2 on t1.seq = t2.id " +
                " where t2.id is null ";
        if (!CollectionUtils.isEmpty(taskNos)) {
            String params = taskNos.stream().map(s -> "'" + s + "'").collect(Collectors.joining(","));
            querySQL = querySQL + " and t1.taskno in (" + params + " )";
        }
        return super.jdbcTemplate.query(querySQL, new BeanPropertyRowMapper<>(MesProcedure.class));
    }
}
