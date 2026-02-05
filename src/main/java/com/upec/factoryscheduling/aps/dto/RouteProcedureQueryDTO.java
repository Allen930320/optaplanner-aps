package com.upec.factoryscheduling.aps.dto;

import com.upec.factoryscheduling.aps.entity.RouteProcedure;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class RouteProcedureQueryDTO extends RouteProcedure implements Serializable {

    private String productCode;
    private String productVersion;
    private String routeCode;
    private String routeName;
    private String routeVersion;

}
