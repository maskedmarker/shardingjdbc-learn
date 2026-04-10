

```text
ShardingSphere-JDBC 的核心原理就是在 JDBC 层做了一层代理，通过解析应用程序的 SQL，根据配置的分片规则动态计算目标数据源和表，改写 SQL 后在真实数据库中执行，最后合并多个结果集返回给应用程序。
整个过程对上层应用完全透明，你依然使用标准的 JDBC 接口，写普通的 SQL，但数据已经自动分布到多个数据库和表中了。
```

```text
ShardingSphere-JDBC 核心实现原理

ShardingSphere-JDBC 的核心原理可以概括为：SQL 解析 → 路由改写 → 执行合并，通过对 JDBC 规范的完整实现，在应用层对数据库进行分片。

应用程序
    ↓
MyBatis/Hibernate (ORM)
    ↓
ShardingSphere-JDBC (增强的 DataSource)
    ├── SQL 解析引擎
    ├── 路由引擎
    ├── 改写引擎
    ├── 执行引擎
    └── 归并引擎
    ↓
真实数据库 (MySQL等)
```

```text
1. SQL 解析引擎
将 SQL 语句解析为抽象语法树 (AST)，提取分片所需信息。


2. 路由引擎
根据分片键计算 SQL 应该路由到哪个数据库和表。

路由类型：
单分片路由：根据分片键值精确计算到一个分片
多分片路由：IN 或 BETWEEN 等条件路由到多个分片
广播路由：全表查询，路由到所有分片
忽略路由：不包含分片键，全路由


3. 改写引擎
将逻辑 SQL 改写为可以在真实数据库中执行的物理 SQL。


4. 执行引擎
在真实数据库中执行改写后的 SQL。

// 执行流程
1. 获取数据库连接 (从 HikariCP 等连接池)
2. 创建 PreparedStatement
3. 设置参数
4. 执行 SQL
5. 处理结果



5. 归并引擎
将多个分片返回的结果进行合并。

-- 逻辑 SQL (查询所有分片)
SELECT * FROM t_order ORDER BY order_id LIMIT 10
-- 实际执行 (ds0.t_order_0, ds0.t_order_1, ds1.t_order_0, ds1.t_order_1)
-- 归并操作：
1. 从每个分片各取 10 条
2. 在内存中进行排序
3. 再次取前 10 条返回
```


```text
 核心源码流程
 
 
关键技术细节 

1. JDBC 规范实现
ShardingSphere 实现了完整的 JDBC 接口：

// 核心类继承关系
ShardingDataSource implements DataSource
    ↓
ShardingConnection implements Connection  
    ↓
ShardingPreparedStatement implements PreparedStatement
    ↓
ShardingResultSet implements ResultSet


批量执行优化
if (同一个数据源 && 同一个连接) {
    批量执行 // 减少网络开销
} else {
    并发执行 // 使用线程池
}
```

```text
与传统方案对比


特性  	    ShardingSphere-JDBC	            MyCat (代理层)
部署方式	    应用内嵌入	                    独立服务
网络开销	    无中间层	                        多一跳
性能	        高	                            中
连接数	    应用数 × 分片数	                分片数 × 连接池
升级维护	    需要重启应用	                    独立升级
```