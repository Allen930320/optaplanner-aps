package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.RouteProcedure;
import com.upec.factoryscheduling.aps.repository.query.RouteProcedureQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteProcedureRepository extends JpaRepository<RouteProcedure,String>, RouteProcedureQuery {
}
