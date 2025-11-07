package com.upec.factoryscheduling.aps.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Entity
@Table(name = "task")
public class Task implements Serializable {

    @Id
    private String taskNo;

    private String orderNo;

    private String status;

    private LocalDateTime factStartDate;

    private LocalDateTime factEndDate;

    private LocalDate planStartDate;

    private LocalDate planEndDate;

    private int priority;
}
