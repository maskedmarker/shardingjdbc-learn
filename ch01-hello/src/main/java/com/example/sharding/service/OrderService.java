package com.example.sharding.service;

import com.example.sharding.entity.Order;
import com.example.sharding.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    
    @Autowired
    private OrderMapper orderMapper;
    
    public int createOrder(Order order) {
        return orderMapper.insert(order);
    }
    
    public Order getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }
    
    public List<Order> getOrdersByUserId(Long userId) {
        return orderMapper.selectByUserId(userId);
    }
}
