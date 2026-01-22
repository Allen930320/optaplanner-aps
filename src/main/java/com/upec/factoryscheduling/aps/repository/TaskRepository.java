package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.Task;
import com.upec.factoryscheduling.aps.repository.query.OrderTaskQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, String>, OrderTaskQuery {

    List<Task> findAllByTaskNoIsIn(List<String> taskNos);
}
