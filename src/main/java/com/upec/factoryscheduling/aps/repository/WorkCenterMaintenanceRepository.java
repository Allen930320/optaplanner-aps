package com.upec.factoryscheduling.aps.repository;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.entity.WorkCenterMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkCenterMaintenanceRepository extends JpaRepository<WorkCenterMaintenance, String> {

    List<WorkCenterMaintenance> findAllByWorkCenterAndCalendarDateGreaterThanEqual(WorkCenter workCenter,LocalDate calendarDate);

    List<WorkCenterMaintenance> findAllByWorkCenter_WorkCenterCodeAndCalendarDateBetween(String workCenterCode, LocalDate start, LocalDate end);

    List<WorkCenterMaintenance> findAllByWorkCenterIsInAndCalendarDateBetween(List<WorkCenter> workCenters, LocalDate start, LocalDate end);

    @Query("SELECT MAX(w.calendarDate) from WorkCenterMaintenance  w")
    LocalDate findFirstByCalendarDateMax();
}
