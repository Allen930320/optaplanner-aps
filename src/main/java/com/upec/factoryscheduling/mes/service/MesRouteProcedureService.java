package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.mes.entity.MesRouteProcedure;
import com.upec.factoryscheduling.mes.repository.MesRouteProcedureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesRouteProcedureService {

    private MesRouteProcedureRepository mesJjRouteProcedureRepository;

    @Autowired
    public void setMesJjRouteProcedureRepository(MesRouteProcedureRepository mesJjRouteProcedureRepository) {
        this.mesJjRouteProcedureRepository = mesJjRouteProcedureRepository;
    }

    public List<MesRouteProcedure> findAllByRouteSeqIn(List<String> routeSeq) {
        return mesJjRouteProcedureRepository.findAllByRouteSeqIn(routeSeq);
    }

    public MesRouteProcedure findById(String id) {
        return mesJjRouteProcedureRepository.findById(id).orElse(null);
    }
}
