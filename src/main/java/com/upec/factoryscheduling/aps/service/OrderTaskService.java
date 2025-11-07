package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.Task;
import com.upec.factoryscheduling.aps.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderTaskService {

    private TaskRepository taskRepository;

    @Autowired
    public void setTaskRepository(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional("h2TransactionManager")
    public List<Task> saveAll(List<Task> tasks) {
        return taskRepository.saveAll(tasks);
    }
}
