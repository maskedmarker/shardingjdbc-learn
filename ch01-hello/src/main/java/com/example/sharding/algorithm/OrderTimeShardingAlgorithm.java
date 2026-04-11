package com.example.sharding.algorithm;

import com.example.sharding.util.OrderIdGenerator;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingValue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 基于订单时间的分表算法
 * 从订单ID中解析时间，按月份路由到不同表
 */
public class OrderTimeShardingAlgorithm {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 精确分片算法
     */
    public static class Precise implements PreciseShardingAlgorithm<Long> {

        @Override
        public String doSharding(Collection<String> availableTargetNames,
                                 PreciseShardingValue<Long> shardingValue) {
            long orderId = shardingValue.getValue();
            String tableSuffix = OrderIdGenerator.getTableSuffixByMonth(orderId);

            // 构建表名：t_order_202401
            String logicTableName = shardingValue.getLogicTableName();
            String actualTableName = logicTableName + "_" + tableSuffix;

            // 检查表是否存在
            for (String targetName : availableTargetNames) {
                if (targetName.equals(actualTableName)) {
                    return targetName;
                }
            }

            // 如果找不到对应月份的表，返回第一个可用表（或抛出异常）
            // 实际生产环境应该提前做好表创建或返回默认表
            return availableTargetNames.iterator().next();
        }
    }

    /**
     * 范围分片算法
     * 根据时间范围返回需要扫描的表
     */
    public static class Range implements RangeShardingAlgorithm<Long> {

        @Override
        public Collection<String> doSharding(Collection<String> availableTargetNames,
                                             RangeShardingValue<Long> shardingValue) {
            Set<String> result = new LinkedHashSet<>();

            // 解析范围的开始和结束时间
            Long lowerEndpoint = shardingValue.getValueRange().hasLowerBound()
                    ? shardingValue.getValueRange().lowerEndpoint()
                    : null;
            Long upperEndpoint = shardingValue.getValueRange().hasUpperBound()
                    ? shardingValue.getValueRange().upperEndpoint()
                    : null;

            if (lowerEndpoint != null && upperEndpoint != null) {
                LocalDateTime startTime = OrderIdGenerator.getOrderTime(lowerEndpoint);
                LocalDateTime endTime = OrderIdGenerator.getOrderTime(upperEndpoint);

                // 生成范围内的所有月份
                LocalDateTime current = startTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                while (!current.isAfter(endTime)) {
                    String tableSuffix = current.format(FORMATTER);
                    String tableName = shardingValue.getLogicTableName() + "_" + tableSuffix;

                    // 只添加存在的表
                    if (availableTargetNames.contains(tableName)) {
                        result.add(tableName);
                    }

                    current = current.plusMonths(1);
                }
            }

            // 如果没有匹配的表，返回所有可用表
            if (result.isEmpty()) {
                return new LinkedHashSet<>(availableTargetNames);
            }

            return result;
        }
    }
}
