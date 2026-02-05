package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import com.upec.factoryscheduling.aps.service.WorkCenterService;
import com.upec.factoryscheduling.mes.entity.MesBaseWorkCenter;
import com.upec.factoryscheduling.mes.repository.MesBaseWorkCenterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
public class MesBaseWorkCenterService {


    private MesBaseWorkCenterRepository mesBaseWorkCenterRepository;

    @Autowired
    public void setMesBaseWorkCenterRepository(MesBaseWorkCenterRepository mesBaseWorkCenterRepository) {
        this.mesBaseWorkCenterRepository = mesBaseWorkCenterRepository;
    }

    private WorkCenterService workCenterService;

    @Autowired
    public void setWorkCenterService(WorkCenterService workCenterService) {
        this.workCenterService = workCenterService;
    }

    @Autowired
    @Qualifier("oracleTemplate")
    protected JdbcTemplate jdbcTemplate;

    public List<MesBaseWorkCenter> findAllByFactorySeq(String factorySeq) {
        return mesBaseWorkCenterRepository.findAllByFactorySeq(factorySeq);
    }

    @Transactional
    public void syncWorkCenterData() {
        String querySQL = " select t1.seq as id, t1.description as name, t1.status, t1.workcenter_code as " +
                " work_center_code " +
                " from mes_base_workcenter t1  " +
                " left join aps_work_center t2 on t2.work_center_code = t1.workcenter_code  " +
                " where t2.work_center_code is null and t1.factory_seq='2'";
        List<WorkCenter> workCenters = jdbcTemplate.query(querySQL, new BeanPropertyRowMapper<>(WorkCenter.class));
        if (!workCenters.isEmpty()) {
            log.info("同步工作中心数据中....");
            for (WorkCenter workCenter : workCenters) {
                workCenter.setCapacity(480);
                workCenter.setStartTime(LocalTime.of(9, 0).toString());
                workCenter.setEndTime(LocalTime.of(17, 0).toString());
            }
            workCenterService.create(workCenters);
        }
    }
}
