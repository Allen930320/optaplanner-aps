package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.dto.RouteProcedureQueryDTO;
import com.upec.factoryscheduling.aps.repository.RouteProcedureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class RouteProcedureService {

    private RouteProcedureRepository routeProcedureRepository;

    @Autowired
    private void setRouteProcedureRepository(RouteProcedureRepository routeProcedureRepository) {
        this.routeProcedureRepository = routeProcedureRepository;
    }
    public Page<RouteProcedureQueryDTO> queryRouteProcedurePage(String productName,
                                                                String productCode,
                                                                String orderNo,
                                                                String taskNo,
                                                                String contractNum,
                                                                Integer pageNum,
                                                                Integer pageSize){
        return routeProcedureRepository.queryRouteProcedurePage(productName,productCode,orderNo,taskNo,contractNum,pageNum,pageSize);
    }
}
