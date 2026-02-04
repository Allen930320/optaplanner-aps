package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.dto.TaskTimeslotDTO;
import com.upec.factoryscheduling.aps.entity.Task;
import com.upec.factoryscheduling.aps.repository.TaskRepository;
import com.upec.factoryscheduling.mes.dto.OrderTaskDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderTaskService {

    private TaskRepository taskRepository;


    @Autowired
    public void setTaskRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional("oracleTransactionManager")
    public List<Task> saveAll(List<Task> tasks) {
        return taskRepository.saveAll(tasks);
    }

    public List<Task> findAllByTaskNoIsIn(List<String> taskNos) {
        return taskRepository.findAllByTaskNoIsIn(taskNos);
    }

    public Map<String, Task> findAllTaskConvertToMap(List<String> taskNos) {
        List<Task> tasks = findAllByTaskNoIsIn(taskNos);
        if (!CollectionUtils.isEmpty(tasks)) {
            return tasks.stream().collect(Collectors.toMap(Task::getTaskNo, task -> task));
        }
        return new HashMap<>();
    }

    public Task findById(String taskNo) {
        return taskRepository.findById(taskNo).orElse(null);
    }

    @Transactional("oracleTransactionManager")
    public void save(Task task) {
        taskRepository.save(task);
    }

    public Page<TaskTimeslotDTO> queryTaskWithTimeslot(String productName,
                                                       String productCode,
                                                       String taskNo,
                                                       String contractNum,
                                                       String startTime,
                                                       String endTime,
                                                       Integer pageNum,
                                                       Integer pageSize) {
        return taskRepository.queryTaskWithTimeslot(productName, productCode, taskNo, contractNum, startTime, endTime, pageNum, pageSize);
    }


    Page<TaskTimeslotDTO> queryTaskWithTimeslotByUser(String productName,
                                                      String productCode,
                                                      String taskNo,
                                                      String contractNum,
                                                      String startTime,
                                                      String endTime,
                                                      Integer pageNum,
                                                      Integer pageSize) {
        return taskRepository.queryTaskWithTimeslotByUser(productName, productCode, taskNo, contractNum, startTime, endTime, pageNum, pageSize);
    }


    public Page<OrderTaskDTO> queryApsTaskPage(String orderName,
                                               String orderNo,
                                               String contractNum,
                                               String startTime,
                                               String endTime,
                                               List<String> statusList,
                                               Integer pageNum,
                                               Integer pageSize) {
        return taskRepository.queryApsTaskPage(orderName,
                orderNo,
                contractNum,
                startTime,
                endTime,
                statusList,
                pageNum,
                pageSize);
    }


    public void lockedScheduling(List<String> taskNos) {
        List<Task> tasks = findAllByTaskNoIsIn(taskNos);
        for (Task task : tasks) {
            task.setLockedRemark(Boolean.TRUE.toString());
        }
        taskRepository.saveAll(tasks);
    }

    public void unlockedScheduling(List<String> taskNos) {
        List<Task> tasks = findAllByTaskNoIsIn(taskNos);
        for (Task task : tasks) {
            task.setLockedRemark(Boolean.FALSE.toString());
        }
        taskRepository.saveAll(tasks);
    }


    public void setPlanStartDate(String taskNo,LocalDate planStartEnd, LocalDate planEndDate) {
        Task task = findById(taskNo);
        if(planStartEnd!=null) {
            task.setPlanStartDate(planStartEnd);
        }
        if(planEndDate!=null) {
            task.setPlanEndDate(planEndDate);
        }
        taskRepository.save(task);
    }
}
