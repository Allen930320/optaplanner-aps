package com.upec.factoryscheduling.mes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "APS_SCHEDULING_WORK")
public class ApsSchedulingWork implements Serializable {
    @Id
    @Column(name = "ID", nullable = false, length = 30)
    private String id;

    @Column(name = "SOLVE_ID", length = 30)
    private String solveId;

    @Column(name = "CREATE_TIME", length = 20)
    private LocalDateTime createTime;

    @Column(name = "SOLVE_STATUS", length = 50)
    private String solveStatus;

    @Column(name = "VERSION", length = 20)
    private String version;

}
