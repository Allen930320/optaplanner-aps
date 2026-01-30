package com.upec.factoryscheduling.mes.repository.query;

import com.upec.factoryscheduling.aps.entity.Order;

import java.util.List;

public interface MesOrderQuery {

    List<Order> queryOrderListNotInApsOrder(List<String> taskNos);
}
