
```text
注意:

1. 无法使用H2作为jdbc-driver, shardingJdbc不支持
2. shardingJdbc的4.x与5.x的写法不一样,需要注意
```


```text
分表数据
curl -X POST http://localhost:9080/orders   -H "Content-Type: application/json"   -d '{"orderId":1001,"userId":500,"orderName":"笔记本电脑","amount":5999.99}'
curl -X POST http://localhost:9080/orders   -H "Content-Type: application/json"   -d '{"orderId":1002,"userId":500,"orderName":"鼠标","amount":89.50}'

分库数据
curl -X POST http://localhost:9080/orders   -H "Content-Type: application/json"   -d '{"orderId":5001,"userId":501,"orderName":"笔记本电脑","amount":5999.99}'
curl -X POST http://localhost:9080/orders   -H "Content-Type: application/json"   -d '{"orderId":5002,"userId":501,"orderName":"鼠标","amount":89.50}'

http://localhost:9080/h2-console
```

```text
Spring Boot 2.3+ 版本对配置属性的命名要求更严格了，需要使用 kebab-case（短横线命名），不能使用下划线。

Description:

Configuration property name 'spring.shardingsphere.datasource.ds_0' is not valid:

    Invalid characters: '_'
    Bean: org.apache.shardingsphere.shardingjdbc.spring.boot.SpringBootConfiguration
    Reason: Canonical names should be kebab-case ('-' separated), lowercase alpha-numeric characters and must start with a letter

Action:

Modify 'spring.shardingsphere.datasource.ds_0' so that it conforms to the canonical names requirements.
```