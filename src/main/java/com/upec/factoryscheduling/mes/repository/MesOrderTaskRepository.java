package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.MesOrderTask;
import com.upec.factoryscheduling.mes.repository.query.MesOrderTaskQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MesOrderTaskRepository extends JpaRepository<MesOrderTask, String>,
        JpaSpecificationExecutor<MesOrderTask>, MesOrderTaskQuery {

    List<MesOrderTask> queryAllByOrderNoInAndTaskStatusIn(Collection<String> orderNos, Collection<String> taskStatuses);

    List<MesOrderTask> queryAllByTaskNoInAndTaskStatusIn(Collection<String> taskNos, Collection<String> taskStatuses);
}
