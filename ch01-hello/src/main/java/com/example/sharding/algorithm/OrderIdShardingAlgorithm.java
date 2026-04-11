package com.example.sharding.algorithm;

import com.example.sharding.util.OrderIdGenerator;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingValue;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * 基于订单ID的分片算法
 * 从订单ID中提取用户ID后缀进行路由
 */
public class OrderIdShardingAlgorithm {

    /**
     * 精确分片算法 - 用于 = 和 IN 操作
     */
    public static class Precise implements PreciseShardingAlgorithm<Long> {

        @Override
        public String doSharding(Collection<String> availableTargetNames,
                                 PreciseShardingValue<Long> shardingValue) {
            long orderId = shardingValue.getValue();
            int dbIndex = OrderIdGenerator.getDbIndex(orderId, availableTargetNames.size());

            // 根据索引选择数据源
            int index = 0;
            for (String targetName : availableTargetNames) {
                if (index == dbIndex) {
                    return targetName;
                }
                index++;
            }

            throw new IllegalStateException("无法找到对应的数据源，orderId: " + orderId);
        }
    }

    /**
     * 范围分片算法 - 用于 BETWEEN 和 > < 操作
     * 注意：订单ID包含时间信息，范围查询可能需要扫描多个库
     */
    public static class Range implements RangeShardingAlgorithm<Long> {

        @Override
        public Collection<String> doSharding(Collection<String> availableTargetNames,
                                             RangeShardingValue<Long> shardingValue) {
            // 范围查询时，需要返回所有可能的数据源
            // 实际业务中可以根据时间范围优化
            return new LinkedHashSet<>(availableTargetNames);
        }
    }
}
