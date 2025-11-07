package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, String> {

}
