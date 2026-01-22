package com.upec.factoryscheduling.aps.repository.query;

import com.upec.factoryscheduling.aps.dto.TaskTimeslotDTO;
import org.springframework.data.domain.Page;

public interface OrderTaskQuery {
    Page<TaskTimeslotDTO> queryTaskWithTimeslot(String productName,
                                                String productCode,
                                                String taskNo,
                                                String contractNum,
                                                String startTime,
                                                String endTime,
                                                Integer pageNum,
                                                Integer pageSize);
}
