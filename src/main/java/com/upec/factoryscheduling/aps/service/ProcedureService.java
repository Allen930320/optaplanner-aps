package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.repository.ProcedureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ProcedureService {

    private ProcedureRepository procedureRepository;

    private OrderTaskService orderTaskService;

    @Autowired
    public void setOrderTaskService(OrderTaskService orderTaskService) {
        this.orderTaskService = orderTaskService;
    }

    @Autowired
    public void setProcedureRepository(ProcedureRepository procedureRepository) {
        this.procedureRepository = procedureRepository;
    }

    @Transactional("oracleTransactionManager")
    public List<Procedure> saveProcedures(List<Procedure> procedures) {
        return procedureRepository.saveAll(procedures);
    }

    @Transactional("oracleTransactionManager")
    public Procedure saveProcedure(Procedure procedure) {
        return procedureRepository.save(procedure);
    }

    @Transactional("oracleTransactionManager")
    public void deleteAll() {
        procedureRepository.deleteAll();
    }

    public List<Procedure> findAllByTaskNoIsIn(List<String> taskNos) {
        return procedureRepository.findAllByTask_TaskNoIsIn(taskNos);
    }

    public Procedure findProcedureById(String id) {
        return procedureRepository.findById(id).orElse(null);
    }


}
