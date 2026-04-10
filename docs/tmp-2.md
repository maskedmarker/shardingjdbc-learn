# 最佳实践

```text
❌ 避免跨分片多表关联查询

-- 错误：t_order 分片，t_user 不分片，但跨库关联
SELECT * FROM t_order o LEFT JOIN t_user u ON o.user_id = u.id WHERE o.order_id = 1;

-- 正确：避免关联查询，改为多次查询
List<Order> orders = orderMapper.selectByOrderId(1);
List<Long> userIds = orders.stream().map(Order::getUserId).collect();
List<User> users = userMapper.selectBatchIds(userIds);
```

```text
❌ 避免跨节点子查询


-- 错误：子查询可能来自不同分片
SELECT * FROM t_order 
WHERE user_id IN (SELECT user_id FROM t_user WHERE age > 18);
-- 结果：无法正确路由

-- 正确：拆分成两次查询
List<Long> userIds = userMapper.selectUserIdsByAge(18);
orders = orderMapper.selectByUserIds(userIds);
```

```text
⚠️ 分片键必须存在


-- 错误：WHERE 条件没有分片键
SELECT * FROM t_order WHERE order_name = '测试';
-- 结果：全路由（查询所有分片），性能极差

-- 正确：WHERE 条件必须包含分片键
SELECT * FROM t_order WHERE order_id = 1 AND order_name = '测试';
-- 结果：精确路由到单个分片
```

```text
分片键的选择原则


# 选择原则：
1. 高频查询字段（90% 以上的查询都包含）
2. 分布均匀（避免数据倾斜）
3. 不可变（尽量不要更新分片键）

# 示例：订单表
# ✅ 好的分片键：user_id（用户查询自己的订单）
# ❌ 差的分片键：order_id（用户不记得订单号）
# ❌ 差的分片键：status（只有几个值，分布不均）
```

## 查询优化注意事项


```text
LIMIT 分页优化
-- ❌ 错误：传统分页（性能差）
SELECT * FROM t_order 
WHERE user_id = 1 
LIMIT 10000, 10;
-- 问题：每个分片都要查询 10010 条，然后归并

-- ✅ 正确：使用游标分页
SELECT * FROM t_order 
WHERE user_id = 1 AND order_id > 10000 
LIMIT 10;
-- 优势：利用主键索引，性能好
```

```text
聚合函数注意事项



-- ⚠️ 注意：COUNT 和 SUM 可以正确归并
SELECT COUNT(*) FROM t_order WHERE user_id = 1;  -- ✅ 正确归并
SELECT SUM(amount) FROM t_order WHERE user_id = 1;  -- ✅ 正确归并

-- ❌ 错误：AVG 不能简单平均
SELECT AVG(amount) FROM t_order WHERE user_id = 1;
-- 原理：ShardingSphere 会改写为 SUM/COUNT

-- ⚠️ 注意：DISTINCT 需要内存去重
SELECT DISTINCT user_id FROM t_order;  -- 全路由 + 内存去重
```

## 🎯 5. 事务注意事项

```text
分布式事务限制


// ❌ 错误：跨库事务需要 XA 或 BASE
@Transactional
public void createOrderAndUpdateUser(Order order, User user) {
    orderMapper.insert(order);  // 可能在 ds0
    userMapper.update(user);    // 可能在 ds1
    // 默认只支持单库事务，跨库会失败
}

// ✅ 正确：避免跨库事务
@Transactional
public void createOrder(Order order) {
    // 确保同一个 user_id 的所有操作在同一库
    orderMapper.insert(order);
    orderLogMapper.insert(log);  // 使用相同 user_id
}

// 或配置 XA 事务
@Bean
public TransactionManager shardingTransactionManager() {
    return new ShardingTransactionManager();
}
```

## 📈 6. 性能优化建议

```text
批量操作优化


// ❌ 错误：循环单条插入（性能差）
for (Order order : orders) {
    orderMapper.insert(order);  // 多次网络往返
}

// ✅ 正确：批量插入（性能好）
orderMapper.insertBatch(orders);  // 一次网络往返

// MyBatis 批量写法
@Insert({"<script>",
    "INSERT INTO t_order (order_id, user_id, amount) VALUES ",
    "<foreach collection='orders' item='item' separator=','>",
    "(#{item.orderId}, #{item.userId}, #{item.amount})",
    "</foreach>",
    "</script>"})
int insertBatch(@Param("orders") List<Order> orders);
```