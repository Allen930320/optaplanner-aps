package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.MesProcedureJoiner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MesProcedureJoinerRepository extends JpaRepository<MesProcedureJoiner, String> {
}
