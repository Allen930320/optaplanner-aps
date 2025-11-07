package com.upec.factoryscheduling.mes.service;

import com.upec.factoryscheduling.mes.entity.MesJjOrderTask;
import com.upec.factoryscheduling.mes.repository.MesJjOrderTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesJjOrderTaskService {

    private MesJjOrderTaskRepository mesJjOrderTaskRepository;

    @Autowired
    private void setMesJjOrderTaskRepository(MesJjOrderTaskRepository mesJjOrderTaskRepository) {
        this.mesJjOrderTaskRepository = mesJjOrderTaskRepository;
    }

    public List<MesJjOrderTask> queryAllByOrderNoInAndTaskStatus(List<String> orderNos, List<String> taskStatus) {
        return mesJjOrderTaskRepository.queryAllByOrderNoInAndTaskStatusIn(orderNos, taskStatus);
    }
}
