package com.upec.factoryscheduling.aps.service;

import com.upec.factoryscheduling.aps.entity.Order;
import com.upec.factoryscheduling.aps.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private OrderRepository orderRepository;

    @Autowired
    public void setOrderRepository(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Optional<Order> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    @Transactional("oracleTransactionManager")
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    @Transactional("oracleTransactionManager")
    public List<Order> createOrders(List<Order> orders) {
        return orderRepository.saveAll(orders);
    }

    @Transactional("oracleTransactionManager")
    public void deleteOrder(String id) {
        orderRepository.deleteById(id);
    }

    @Transactional("oracleTransactionManager")
    public void deleteAll() {
        orderRepository.deleteAll();
    }

    @Transactional("oracleTransactionManager")
    public List<Order> saveAll(List<Order> orders) {
        return orderRepository.saveAll(orders);
    }

    public List<Order> findAllByOrderNoIn(List<String> orderNos) {
        return orderRepository.findByOrderNoIn(orderNos);
    }


    public Map<String, Order> findAllByOrderNoInConvertToMap(List<String> orderNos) {
        List<Order> orders = orderRepository.findByOrderNoIn(orderNos);
        if (!CollectionUtils.isEmpty(orders)) {
            return orders.stream().collect(Collectors.toMap(Order::getOrderNo, m -> m));
        }
        return new HashMap<>();
    }
}
