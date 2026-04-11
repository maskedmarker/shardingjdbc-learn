package com.example.sharding.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * 订单ID生成器演示程序
 * 用于验证生成器的功能和性能
 */
public class OrderIdGeneratorDemo {

    public static void main(String[] args) {
        System.out.println("========== 订单ID生成器演示 ==========\n");

        // 1. 基本生成演示
        demoBasicGeneration();

        // 2. 解析演示
        demoParsing();

        // 3. 分片路由演示
        demoSharding();

        // 4. 唯一性验证
        demoUniqueness();

        // 5. 性能测试
        demoPerformance();

        // 6. 数据分布测试
        demoDistribution();

        System.out.println("\n========== 演示结束 ==========");
    }

    /**
     * 基本生成演示
     */
    private static void demoBasicGeneration() {
        System.out.println("【1. 基本生成演示】");
        System.out.println("-----------------------------------");

        long[] userIds = {10001L, 10002L, 10003L, 10004L};

        for (long userId : userIds) {
            long orderId = OrderIdGenerator.generate(userId);
            System.out.printf("用户ID: %d -> 订单ID: %d (长度: %d)%n",
                    userId, orderId, String.valueOf(orderId).length());
        }
        System.out.println();
    }

    /**
     * 解析演示
     */
    private static void demoParsing() {
        System.out.println("【2. 订单ID解析演示】");
        System.out.println("-----------------------------------");

        long userId = 123456789L;
        long orderId = OrderIdGenerator.generate(userId);

        System.out.println(OrderIdGenerator.format(orderId));
        System.out.println();
    }

    /**
     * 分片路由演示
     */
    private static void demoSharding() {
        System.out.println("【3. 分片路由演示】");
        System.out.println("-----------------------------------");

        // 模拟 2 库 2 表的场景
        int dbCount = 2;
        int tableCount = 2;

        System.out.println("假设配置: " + dbCount + " 库，每库 " + tableCount + " 表");
        System.out.println();

        long[] testUserIds = {1L, 2L, 3L, 4L, 1000001L, 1000002L};

        System.out.println("用户ID -> 订单ID -> 路由结果:");
        for (long userId : testUserIds) {
            long orderId = OrderIdGenerator.generate(userId);
            int dbIndex = OrderIdGenerator.getDbIndex(orderId, dbCount);
            int tableIndex = OrderIdGenerator.getTableIndexByUser(orderId, tableCount);
            String monthSuffix = OrderIdGenerator.getTableSuffixByMonth(orderId);

            System.out.printf("  用户 %d -> 订单 %d -> ds%d.t_order_%d (月份: %s)%n",
                    userId, orderId, dbIndex, tableIndex, monthSuffix);
        }
        System.out.println();
    }

    /**
     * 唯一性验证
     */
    private static void demoUniqueness() {
        System.out.println("【4. 唯一性验证】");
        System.out.println("-----------------------------------");

        int count = 10000;
        Set<Long> orderIds = new HashSet<>(count);
        long userId = 12345L;
        boolean allUnique = true;

        for (int i = 0; i < count; i++) {
            long orderId = OrderIdGenerator.generate(userId);
            if (!orderIds.add(orderId)) {
                System.out.println("发现重复ID: " + orderId);
                allUnique = false;
                break;
            }
        }

        System.out.printf("生成 %d 个订单ID，唯一性: %s%n", count, allUnique ? "通过" : "失败");
        System.out.println();
    }

    /**
     * 性能测试
     */
    private static void demoPerformance() {
        System.out.println("【5. 性能测试】");
        System.out.println("-----------------------------------");

        int[] testCounts = {10000, 100000};
        long userId = 777777L;

        for (int count : testCounts) {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
                OrderIdGenerator.generate(userId);
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            double tps = (count * 1000.0) / duration;

            System.out.printf("生成 %,d 个订单ID，耗时 %d ms，TPS: %,.2f%n",
                    count, duration, tps);
        }
        System.out.println();
    }

    /**
     * 数据分布测试
     */
    private static void demoDistribution() {
        System.out.println("【6. 数据分布测试】");
        System.out.println("-----------------------------------");

        int dbCount = 2;
        int tableCount = 2;
        int userCount = 1000;

        int[] dbDistribution = new int[dbCount];
        int[][] tableDistribution = new int[dbCount][tableCount];

        for (long userId = 1; userId <= userCount; userId++) {
            long orderId = OrderIdGenerator.generate(userId);
            int dbIndex = OrderIdGenerator.getDbIndex(orderId, dbCount);
            int tableIndex = OrderIdGenerator.getTableIndexByUser(orderId, tableCount);

            dbDistribution[dbIndex]++;
            tableDistribution[dbIndex][tableIndex]++;
        }

        System.out.println("分库分布:");
        for (int i = 0; i < dbCount; i++) {
            double percentage = dbDistribution[i] * 100.0 / userCount;
            System.out.printf("  ds%d: %d (%.2f%%)%n", i, dbDistribution[i], percentage);
        }

        System.out.println("\n分表分布:");
        for (int i = 0; i < dbCount; i++) {
            for (int j = 0; j < tableCount; j++) {
                System.out.printf("  ds%d.t_order_%d: %d%n", i, j, tableDistribution[i][j]);
            }
        }
        System.out.println();
    }
}
