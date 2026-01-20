package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.mes.dto.ProcedureQueryDTO;
import com.upec.factoryscheduling.mes.entity.MesJjProcedure;
import com.upec.factoryscheduling.mes.repository.MesProcedureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesJjProcedureService {

    private MesProcedureRepository mesJjProcedureRepository;

    @Autowired
    public void setMesJjProcedureRepository(MesProcedureRepository mesJjProcedureRepository) {
        this.mesJjProcedureRepository = mesJjProcedureRepository;
    }


    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("oracleTemplate")
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MesJjProcedure> findAllByTaskNo(List<String> taskNos) {
        return mesJjProcedureRepository.findAllByTaskNoIn(taskNos);
    }

    public void syncProcedures() {
        String sql = " SELECT T1.* FROM MES_JJ_PROCEDURE T1 " +
                " INNER JOIN MES_JJ_ORDER T2 ON T2.ORDER_STATUS <> '生产完成' AND T1.ORDERNO = T2.ORDERNO " +
                " WHERE T1.CREATEDATE>='2025-01-01' AND T1.CREATEDATE<= '2025-01-31' " +
                " AND T1.SEQ NOT IN ( SELECT ID FROM APS_PROCEDURE ) ";
        List<MesJjProcedure> mesJjProcedures = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(MesJjProcedure.class));
    }

    public Page<ProcedureQueryDTO> queryProcedures(String orderName,
                                                   String taskNo,
                                                   String contractNum,
                                                   String productCode,
                                                   List<String> statusList,
                                                   String startDate,
                                                   String endDate,
                                                   Integer pageNum,
                                                   Integer pageSize) {
        return mesJjProcedureRepository.procedureQueryDTOPage(orderName, taskNo, contractNum, productCode, statusList, startDate, endDate, pageNum, pageSize);
    }
}
