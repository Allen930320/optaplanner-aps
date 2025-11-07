package com.upec.factoryscheduling.aps.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.optaplanner.core.api.domain.lookup.PlanningId;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@Setter
@Getter
public class Order {


    @Id
    private String orderNo;

    private String erpStatus;

    private String orderStatus;

    private LocalDate planStartDate;

    private LocalDate planEndDate;

    private LocalDateTime factStartDate;

    private LocalDateTime factEndDate;
}
