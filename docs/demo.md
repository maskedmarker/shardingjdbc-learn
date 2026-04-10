
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