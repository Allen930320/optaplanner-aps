package com.upec.factoryscheduling.aps.resquest;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Data
public class ProcedureRequest {

    private String orderNo;
    private String machineNo;
    private Integer procedureNo;
    private LocalDateTime date;
    // 分页查询参数
    private Integer pageNum;
    private Integer pageSize;
    // 工序查询参数
    private String taskNo;
    private String status;
}
