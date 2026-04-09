package com.example.sharding.config;

import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class ShardingConfig {

    @Bean
    public DataSource dataSource() throws SQLException {
        // 配置分片规则
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        
        // 配置分表规则
        ShardingTableRuleConfiguration orderTableRule = new ShardingTableRuleConfiguration("t_order", "ds_${0..1}.t_order_${0..1}");
        orderTableRule.setTableShardingStrategy(new StandardShardingStrategyConfiguration(
                "order_id", "order_table_sharding_algorithm"));
        orderTableRule.setDatabaseShardingStrategy(new StandardShardingStrategyConfiguration(
                "user_id", "order_database_sharding_algorithm"));
        
        shardingRuleConfig.getTables().add(orderTableRule);
        
        // 配置分片算法
        Properties props = new Properties();
        props.setProperty("order_table_sharding_algorithm.type", "MOD");
        props.setProperty("order_table_sharding_algorithm.props.shardingCount", "2");
        props.setProperty("order_database_sharding_algorithm.type", "MOD");
        props.setProperty("order_database_sharding_algorithm.props.shardingCount", "2");
        
        // 配置数据源
        Map<String, DataSource> dataSources = new HashMap<>();
        dataSources.put("ds_0", createDataSource("ds_0"));
        dataSources.put("ds_1", createDataSource("ds_1"));
        
        // 将分片规则配置添加到集合中
        java.util.Collection<org.apache.shardingsphere.infra.config.rule.RuleConfiguration> ruleConfigurations = new java.util.ArrayList<>();
        ruleConfigurations.add(shardingRuleConfig);
        
        return ShardingSphereDataSourceFactory.createDataSource(dataSources, ruleConfigurations, props);
    }
    
    private DataSource createDataSource(String databaseName) {
        com.zaxxer.hikari.HikariDataSource dataSource = new com.zaxxer.hikari.HikariDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/" + databaseName + "?useSSL=false&serverTimezone=UTC");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        return dataSource;
    }
}
