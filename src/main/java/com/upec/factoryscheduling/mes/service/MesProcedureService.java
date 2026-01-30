package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.mes.dto.ProcedureQueryDTO;
import com.upec.factoryscheduling.mes.entity.MesProcedure;
import com.upec.factoryscheduling.mes.repository.MesProcedureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesProcedureService {

    private MesProcedureRepository mesProcedureRepository;

    @Autowired
    public void setMesJjProcedureRepository(MesProcedureRepository mesProcedureRepository) {
        this.mesProcedureRepository = mesProcedureRepository;
    }

    public List<MesProcedure> findAllByTaskNo(List<String> taskNos) {
        return mesProcedureRepository.findAllByTaskNoIn(taskNos);
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
        return mesProcedureRepository.procedureQueryDTOPage(orderName, taskNo, contractNum, productCode, statusList, startDate, endDate, pageNum, pageSize);
    }


    public List<MesProcedure> queryMesProcedureNotInAps(List<String> taskNos){
        return mesProcedureRepository.findAllByTaskNoIn(taskNos);
    }
}
