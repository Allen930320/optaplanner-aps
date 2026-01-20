package com.upec.factoryscheduling.mes.repository.query;


import com.upec.factoryscheduling.mes.dto.OrderTaskDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MesOrderTaskQuery {

    Page<OrderTaskDTO> queryOrderTaskForPage(String orderName,
                                             String orderNo,
                                             String contractNum,
                                             String startTime,
                                             String endTime,
                                             List<String> statusList,
                                             Integer pageNum,
                                             Integer pageSize);
}
