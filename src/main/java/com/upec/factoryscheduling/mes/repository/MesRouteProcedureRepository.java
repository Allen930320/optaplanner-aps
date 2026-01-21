package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.MesRouteProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesRouteProcedureRepository extends JpaRepository<MesRouteProcedure, String> {
    List<MesRouteProcedure> findAllByRouteSeqIn(List<String> routeSeq);
}
