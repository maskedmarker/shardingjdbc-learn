package com.example.sharding.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单ID生成器
 * 采用淘宝风格设计：19位数字
 * 结构：时间(13位) + 序列号(3位) + 用户ID后缀(6位) = 19位
 *
 * 优势：
 * 1. 从订单号可直接解析下单时间，便于按时间分表定位
 * 2. 包含用户ID信息，可直接用于分库路由
 * 3. 全局唯一，支持高并发
 */
public class OrderIdGenerator {

    // 序列号，每毫秒自增
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    // 序列号最大值（3位，0-999）
    private static final int MAX_SEQUENCE = 999;

    // 上一次生成ID的时间戳
    private static volatile long lastTimestamp = -1L;

    // 用户ID后缀位数
    private static final int USER_SUFFIX_LENGTH = 6;

    // 用户ID后缀最大值
    private static final long MAX_USER_SUFFIX = 1_000_000L;

    /**
     * 生成订单ID
     *
     * @param userId 用户ID
     * @return 19位订单ID
     */
    public static synchronized long generate(long userId) {
        long currentTimestamp = System.currentTimeMillis();

        // 如果当前时间小于上一次生成时间，说明时钟回拨，抛出异常
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate id for "
                    + (lastTimestamp - currentTimestamp) + " milliseconds");
        }

        // 如果是同一毫秒内，序列号自增
        if (currentTimestamp == lastTimestamp) {
            int sequence = SEQUENCE.incrementAndGet();
            if (sequence > MAX_SEQUENCE) {
                // 序列号溢出，等待下一毫秒
                currentTimestamp = tilNextMillis(lastTimestamp);
                SEQUENCE.set(0);
            }
        } else {
            // 不同毫秒，重置序列号
            SEQUENCE.set(0);
        }

        lastTimestamp = currentTimestamp;

        // 获取用户ID后6位
        long userSuffix = userId % MAX_USER_SUFFIX;

        // 组合订单ID：时间戳(13位) * 10^9 + 序列号(3位) * 10^6 + 用户后缀(6位)
        return currentTimestamp * 1_000_000_000L + SEQUENCE.get() * MAX_USER_SUFFIX + userSuffix;
    }

    /**
     * 等待下一毫秒
     */
    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 从订单ID解析用户ID后缀（用于分库路由）
     *
     * @param orderId 订单ID
     * @return 用户ID后缀（6位）
     */
    public static long getUserSuffix(long orderId) {
        return orderId % MAX_USER_SUFFIX;
    }

    /**
     * 从订单ID解析下单时间戳
     *
     * @param orderId 订单ID
     * @return 时间戳（毫秒）
     */
    public static long getTimestamp(long orderId) {
        return orderId / 1_000_000_000L;
    }

    /**
     * 从订单ID解析下单时间
     *
     * @param orderId 订单ID
     * @return LocalDateTime
     */
    public static LocalDateTime getOrderTime(long orderId) {
        long timestamp = getTimestamp(orderId);
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
    }

    /**
     * 从订单ID解析序列号
     *
     * @param orderId 订单ID
     * @return 序列号（0-999）
     */
    public static int getSequence(long orderId) {
        return (int) ((orderId / MAX_USER_SUFFIX) % 1000);
    }

    /**
     * 根据订单ID获取分库索引
     *
     * @param orderId 订单ID
     * @param dbCount 数据库数量
     * @return 数据库索引
     */
    public static int getDbIndex(long orderId, int dbCount) {
        return (int) (getUserSuffix(orderId) % dbCount);
    }

    /**
     * 根据订单ID获取分表索引（按用户ID取模）
     *
     * @param orderId   订单ID
     * @param tableCount 表数量
     * @return 表索引
     */
    public static int getTableIndexByUser(long orderId, int tableCount) {
        return (int) (getUserSuffix(orderId) % tableCount);
    }

    /**
     * 根据订单ID获取分表索引（按时间月份）
     *
     * @param orderId 订单ID
     * @return 表名后缀，如 "202401"
     */
    public static String getTableSuffixByMonth(long orderId) {
        LocalDateTime orderTime = getOrderTime(orderId);
        return orderTime.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    /**
     * 格式化订单ID（便于阅读）
     *
     * @param orderId 订单ID
     * @return 格式化后的字符串
     */
    public static String format(long orderId) {
        return String.format("OrderId: %d\n" +
                        "  - 时间: %s\n" +
                        "  - 序列号: %d\n" +
                        "  - 用户后缀: %06d\n" +
                        "  - 建议分库: ds%d\n" +
                        "  - 建议分表(按用户): t_order_%d\n" +
                        "  - 建议分表(按月): t_order_%s",
                orderId,
                getOrderTime(orderId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")),
                getSequence(orderId),
                getUserSuffix(orderId),
                getDbIndex(orderId, 2),
                getTableIndexByUser(orderId, 2),
                getTableSuffixByMonth(orderId)
        );
    }
}
