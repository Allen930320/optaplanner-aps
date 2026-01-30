package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.MesOrder;
import com.upec.factoryscheduling.mes.repository.query.MesOrderQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MesOrderRepository extends JpaRepository<MesOrder, String> , MesOrderQuery {

}
