package com.example.sharding.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单ID生成器测试类
 */
class OrderIdGeneratorTest {

    /**
     * 测试基本生成功能
     */
    @Test
    void testGenerate() {
        long userId = 123456789L;
        long orderId = OrderIdGenerator.generate(userId);

        System.out.println("生成的订单ID: " + orderId);
        System.out.println("订单ID长度: " + String.valueOf(orderId).length());
        System.out.println(OrderIdGenerator.format(orderId));

        // 验证长度
        assertEquals(19, String.valueOf(orderId).length(), "订单ID应为19位");
    }

    /**
     * 测试用户ID后缀提取
     */
    @Test
    void testGetUserSuffix() {
        long userId = 123456789L;
        long orderId = OrderIdGenerator.generate(userId);
        long userSuffix = OrderIdGenerator.getUserSuffix(orderId);

        // 用户ID后6位应该是 567890
        assertEquals(userId % 1_000_000, userSuffix);
        System.out.println("用户ID: " + userId);
        System.out.println("用户后缀: " + userSuffix);
    }

    /**
     * 测试时间解析
     */
    @Test
    void testGetOrderTime() {
        long userId = 10086L;
        long beforeGenerate = System.currentTimeMillis();
        long orderId = OrderIdGenerator.generate(userId);
        long afterGenerate = System.currentTimeMillis();

        long timestamp = OrderIdGenerator.getTimestamp(orderId);
        LocalDateTime orderTime = OrderIdGenerator.getOrderTime(orderId);

        // 验证时间在生成前后之间
        assertTrue(timestamp >= beforeGenerate && timestamp <= afterGenerate,
                "订单时间应在生成时间范围内");

        System.out.println("订单时间: " + orderTime);
    }

    /**
     * 测试分库路由
     */
    @Test
    void testGetDbIndex() {
        // 测试不同用户ID的路由结果
        long[] userIds = {1L, 2L, 3L, 4L, 1000001L, 1000002L};

        for (long userId : userIds) {
            long orderId = OrderIdGenerator.generate(userId);
            int dbIndex = OrderIdGenerator.getDbIndex(orderId, 2);
            long userSuffix = OrderIdGenerator.getUserSuffix(orderId);

            System.out.printf("用户ID: %d, 用户后缀: %d, 路由到库: ds%d%n",
                    userId, userSuffix, dbIndex);

            // 验证路由一致性：相同用户后缀应路由到同一库
            assertEquals((int) (userSuffix % 2), dbIndex);
        }
    }

    /**
     * 测试分表路由（按用户）
     */
    @Test
    void testGetTableIndexByUser() {
        long userId = 888888L;
        long orderId = OrderIdGenerator.generate(userId);

        int tableIndex = OrderIdGenerator.getTableIndexByUser(orderId, 2);
        long userSuffix = OrderIdGenerator.getUserSuffix(orderId);

        System.out.printf("用户后缀: %d, 路由到表: t_order_%d%n", userSuffix, tableIndex);
        assertEquals((int) (userSuffix % 2), tableIndex);
    }

    /**
     * 测试分表路由（按月）
     */
    @Test
    void testGetTableSuffixByMonth() {
        long userId = 999999L;
        long orderId = OrderIdGenerator.generate(userId);

        String tableSuffix = OrderIdGenerator.getTableSuffixByMonth(orderId);
        LocalDateTime orderTime = OrderIdGenerator.getOrderTime(orderId);

        String expectedSuffix = String.format("%d%02d", orderTime.getYear(), orderTime.getMonthValue());

        System.out.printf("订单时间: %s, 表后缀: %s%n", orderTime, tableSuffix);
        assertEquals(expectedSuffix, tableSuffix);
    }

    /**
     * 测试唯一性 - 单线程生成大量ID
     */
    @Test
    void testUniqueness() {
        int count = 10000;
        Set<Long> orderIds = new HashSet<>(count);
        long userId = 12345L;

        for (int i = 0; i < count; i++) {
            long orderId = OrderIdGenerator.generate(userId);
            assertTrue(orderIds.add(orderId), "订单ID应唯一，发现重复: " + orderId);
        }

        System.out.println("生成 " + count + " 个订单ID，全部唯一");
    }

    /**
     * 测试唯一性 - 多线程并发
     */
    @Test
    void testConcurrentUniqueness() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        int idsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<Long> allOrderIds = ConcurrentHashMap.newKeySet();

        Future<?>[] futures = new Future[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1;
            futures[i] = executor.submit(() -> {
                for (int j = 0; j < idsPerThread; j++) {
                    long orderId = OrderIdGenerator.generate(userId);
                    assertTrue(allOrderIds.add(orderId),
                            "并发环境下订单ID应唯一，发现重复: " + orderId);
                }
            });
        }

        // 等待所有线程完成
        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();

        System.out.println("并发测试: " + threadCount + " 个线程，每线程 "
                + idsPerThread + " 个ID，共生成 " + allOrderIds.size() + " 个唯一ID");

        assertEquals(threadCount * idsPerThread, allOrderIds.size());
    }

    /**
     * 测试性能
     */
    @Test
    void testPerformance() {
        int count = 100000;
        long userId = 777777L;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            OrderIdGenerator.generate(userId);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.printf("生成 %d 个订单ID，耗时 %d ms，TPS: %.2f%n",
                count, duration, (count * 1000.0) / duration);

        // 验证性能：每秒至少生成 10000 个
        assertTrue(duration < 10000, "生成10万订单ID应在10秒内完成");
    }

    /**
     * 测试不同用户ID的订单ID分布
     */
    @Test
    void testDistribution() {
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

        System.out.println("=== 数据分布统计 ===");
        System.out.println("分库分布:");
        for (int i = 0; i < dbCount; i++) {
            System.out.printf("  ds%d: %d (%.2f%%)%n", i, dbDistribution[i],
                    dbDistribution[i] * 100.0 / userCount);
        }

        System.out.println("\n分表分布:");
        for (int i = 0; i < dbCount; i++) {
            for (int j = 0; j < tableCount; j++) {
                System.out.printf("  ds%d.t_order_%d: %d%n", i, j, tableDistribution[i][j]);
            }
        }

        // 验证分布均匀性（每个库应该在 45%-55% 之间）
        for (int count : dbDistribution) {
            double ratio = count * 100.0 / userCount;
            assertTrue(ratio > 45 && ratio < 55, "数据分布应相对均匀");
        }
    }

    /**
     * 演示完整使用流程
     */
    @Test
    void testFullWorkflow() {
        System.out.println("========== 订单ID生成器演示 ==========\n");

        // 模拟不同用户下单
        long[] userIds = {10001L, 10002L, 10003L, 10004L};

        for (long userId : userIds) {
            long orderId = OrderIdGenerator.generate(userId);

            System.out.println("-----------------------------------");
            System.out.println(OrderIdGenerator.format(orderId));
            System.out.println();
        }

        System.out.println("========== 演示结束 ==========");
    }
}
