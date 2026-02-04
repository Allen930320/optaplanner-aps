package com.upec.factoryscheduling.aps.repository.query;

import com.upec.factoryscheduling.aps.dto.TaskTimeslotDTO;
import com.upec.factoryscheduling.mes.dto.OrderTaskDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderTaskQuery {
    Page<TaskTimeslotDTO> queryTaskWithTimeslot(String productName,
                                                String productCode,
                                                String taskNo,
                                                String contractNum,
                                                String startTime,
                                                String endTime,
                                                Integer pageNum,
                                                Integer pageSize);

    Page<TaskTimeslotDTO> queryTaskWithTimeslotByUser(String productName,
                                                      String productCode,
                                                      String taskNo,
                                                      String contractNum,
                                                      String startTime,
                                                      String endTime,
                                                      Integer pageNum,
                                                      Integer pageSize);

    Page<OrderTaskDTO> queryApsTaskPage(String orderName,
                                        String orderNo,
                                        String contractNum,
                                        String startTime,
                                        String endTime,
                                        List<String> statusList,
                                        Integer pageNum,
                                        Integer pageSize);
}
