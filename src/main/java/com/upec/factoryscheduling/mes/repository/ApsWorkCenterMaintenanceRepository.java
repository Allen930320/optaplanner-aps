package com.upec.factoryscheduling.mes.repository;

import com.upec.factoryscheduling.mes.entity.ApsWorkCenterMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApsWorkCenterMaintenanceRepository extends JpaRepository<ApsWorkCenterMaintenance, String> {

    List<ApsWorkCenterMaintenance> findAllByWorkCenterCodeIn(List<String> workCenterCodes);
}
