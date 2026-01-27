package com.upec.factoryscheduling.mes.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "MES_JJ_PROCEDURE_JOINER")
@Entity
@Data
public class MesProcedureJoiner {

    @Id
    @Column(name = "SEQ")
    private String seq;
    @Column(name = "PROCEDURE_SEQ")
    private String procedureSeq;
    @Column(name = "PRODUCT_USER")
    private String productUser;
    @Column(name = "CREATEUSER")
    private String createUser;
    @Column(name = "CREATEDATE")
    private String createDate;

}
