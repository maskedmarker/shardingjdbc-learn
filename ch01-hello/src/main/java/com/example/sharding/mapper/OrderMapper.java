package com.example.sharding.mapper;

import com.example.sharding.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderMapper {
    
    @Insert("INSERT INTO t_order (order_id, user_id, order_name, amount) VALUES (#{orderId}, #{userId}, #{orderName}, #{amount})")
    int insert(Order order);
    
    @Select("SELECT * FROM t_order WHERE order_id = #{orderId}")
    Order selectById(Long orderId);
    
    @Select("SELECT * FROM t_order WHERE user_id = #{userId}")
    List<Order> selectByUserId(Long userId);
}
