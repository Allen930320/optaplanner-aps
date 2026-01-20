package com.upec.factoryscheduling.mes.repository.query;

import com.upec.factoryscheduling.mes.dto.ProcedureQueryDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MesProcedureQuery {
    Page<ProcedureQueryDTO> procedureQueryDTOPage(String orderName,
                                                  String taskNo,
                                                  String contractNum,
                                                  String productCode,
                                                  List<String> statusList,
                                                  String startDate,
                                                  String endDate,
                                                  Integer pageNum,
                                                  Integer pageSize);
}
