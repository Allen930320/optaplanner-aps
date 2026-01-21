package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.MesProcedure;
import com.upec.factoryscheduling.mes.repository.query.MesProcedureQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MesProcedureRepository extends JpaRepository<MesProcedure, String>, MesProcedureQuery {

    List<MesProcedure> findAllByTaskNoIn(List<String> taskNos);

}
