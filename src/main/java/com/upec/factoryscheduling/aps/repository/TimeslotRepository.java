package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.Procedure;
import com.upec.factoryscheduling.aps.entity.Timeslot;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TimeslotRepository extends JpaRepository<Timeslot, String>, JpaSpecificationExecutor<Timeslot> {

    List<Timeslot> findAllByProcedure_Task_TaskNoIsIn(List<String> taskNos);

    List<Timeslot> findAllByProcedure_Task_TaskNoIsIn(List<String> taskNos, Sort sort);

    List<Timeslot> findAllByProcedure(Procedure procedure);

    List<Timeslot> findAllByIdIsIn(Collection<String> ids);

    List<Timeslot> findAllByProcedureAndIdNot(Procedure procedure, String id);

    @Query("select distinct t1 from Timeslot t1 inner join MesProcedure t2 on t1.procedure.id=t2.seq " +
            " left join MesProcedureJoiner t3 on t3.procedureSeq=t2.seq " +
            " where ((t2.qualityUser like %:user% and t1.procedure.status in ('待质检','质检中')) or " +
            " (t1.procedure.status in ('执行中', '待执行', '初始导入') and t3.productUser = :user ) )" +
            " and t2.taskNo in (:taskNos)")
    List<Timeslot> queryTimeslots(@Param("user") String user, @Param("taskNos") List<String> taskNos);

}
