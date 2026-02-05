package com.upec.factoryscheduling.aps.repository.query;

import com.upec.factoryscheduling.aps.dto.RouteProcedureQueryDTO;
import com.upec.factoryscheduling.common.utils.JdbcTemplatePagination;
import org.springframework.data.domain.Page;

public interface RouteProcedureQuery  {
    Page<RouteProcedureQueryDTO> queryRouteProcedurePage(String productName,
                                                         String productCode,
                                                         String orderNo,
                                                         String taskNo,
                                                         String contractNum,
                                                         Integer pageNum,
                                                         Integer pageSize);
}
