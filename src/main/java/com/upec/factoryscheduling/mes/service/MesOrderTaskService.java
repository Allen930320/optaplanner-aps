package com.upec.factoryscheduling.mes.service;


import com.upec.factoryscheduling.aps.entity.Task;
import com.upec.factoryscheduling.mes.dto.OrderTaskDTO;
import com.upec.factoryscheduling.mes.entity.MesOrderTask;
import com.upec.factoryscheduling.mes.repository.MesOrderTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesOrderTaskService {

    private MesOrderTaskRepository mesJjOrderTaskRepository;


    @Autowired
    private void setMesJjOrderTaskRepository(MesOrderTaskRepository mesJjOrderTaskRepository) {
        this.mesJjOrderTaskRepository = mesJjOrderTaskRepository;
    }

    public Page<OrderTaskDTO> queryOrderTaskForPage(String orderName,
                                                    String orderNo,
                                                    String contractNum,
                                                    String startTime,
                                                    String endTime,
                                                    List<String> statusList,
                                                    Integer pageNum,
                                                    Integer pageSize) {
        return mesJjOrderTaskRepository.queryOrderTaskForPage(
                orderName,
                orderNo,
                contractNum,
                startTime,
                endTime,
                statusList,
                pageNum,
                pageSize);
    }

    public List<Task> queryTaskListNotInApsTask(List<String> taskNos) {
        return mesJjOrderTaskRepository.queryTaskListNotInApsTask(taskNos);
    }
}
