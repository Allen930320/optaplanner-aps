package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.repository.query.WorkCenterQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkCenterRepository extends JpaRepository<WorkCenter, String>, WorkCenterQuery {

    List<WorkCenter> findAllByWorkCenterCode(String workCenterCode);

    @Query("select t1 from WorkCenter t1 where t1.name like %:name% or t1.workCenterCode like %:workCenterCode% ")
    List<WorkCenter> findAllByNameLikeOrWorkCenterCodeLike(@Param("name") String name,
                                                           @Param(("workCenterCode")) String workCenterCode);
}
