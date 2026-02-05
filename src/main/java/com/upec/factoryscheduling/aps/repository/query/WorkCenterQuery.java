package com.upec.factoryscheduling.aps.repository.query;

import com.upec.factoryscheduling.aps.entity.WorkCenter;
import org.springframework.data.domain.Page;

public interface WorkCenterQuery {
    Page<WorkCenter> queryWorkCenterPage(String name, String code, Integer pageNum, Integer pageSize);
}
