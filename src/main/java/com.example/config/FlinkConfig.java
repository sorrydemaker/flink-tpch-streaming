package com.example.config;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.springframework.context.annotation.Bean;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.RestOptions;
import java.time.Duration;
import java.util.Properties;

@org.springframework.context.annotation.Configuration
public class FlinkConfig {

    private Properties kafkaProperties;

    public FlinkConfig() {
        this.kafkaProperties = new Properties();
        kafkaProperties.setProperty("bootstrap.servers", "localhost:9092");
        kafkaProperties.setProperty("group.id", "tpch-query-group");
        kafkaProperties.setProperty("auto.offset.reset", "earliest");
    }

    @Bean
    public StreamExecutionEnvironment streamExecutionEnvironment() {
        Configuration configuration = new Configuration();
        configuration.setString("group.id", "tpch-query-group");
        configuration.setString("bootstrap.servers", "localhost:9092");
        configuration.setString("auto.offset.reset", "earliest");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(configuration);
        env.getConfig().setGlobalJobParameters(configuration);
        return env;
    }

    @Bean
    public StreamTableEnvironment streamTableEnvironment(
            StreamExecutionEnvironment streamExecutionEnvironment) {
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .build();

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(
                streamExecutionEnvironment,
                settings
        );

        tableEnv.getConfig().set("table.exec.source.idle-timeout", "0");
        tableEnv.getConfig().set("kafka.group.id", "tpch-query-group");

        registerTables(tableEnv);
        return tableEnv;
    }

    private void registerTables(StreamTableEnvironment tableEnv) {
        tableEnv.executeSql(
                "CREATE TABLE lineitem (" +
                        "    l_orderkey    BIGINT," +
                        "    l_partkey     BIGINT," +
                        "    l_suppkey     BIGINT," +
                        "    l_linenumber  INTEGER," +
                        "    l_quantity    DECIMAL(15, 2)," +
                        "    l_extendedprice  DECIMAL(15, 2)," +
                        "    l_discount    DECIMAL(15, 2)," +
                        "    l_tax         DECIMAL(15, 2)," +
                        "    l_returnflag  STRING," +
                        "    l_linestatus  STRING," +
                        "    l_shipdate    DATE," +
                        "    l_commitdate  DATE," +
                        "    l_receiptdate DATE," +
                        "    l_shipinstruct STRING," +
                        "    l_shipmode     STRING," +
                        "    l_comment      STRING," +
                        "    proctime AS PROCTIME()" +
                        ") WITH (" +
                        "    'connector' = 'datagen'," +
                        "    'rows-per-second' = '100'," +
                        "    'fields.l_orderkey.min' = '1'," +
                        "    'fields.l_orderkey.max' = '1000'," +
                        "    'properties.group.id' = 'tpch-query-group'," +
                        "    'properties.auto.offset.reset' = 'earliest'" +
                        ")"
        );

        tableEnv.executeSql(
                "CREATE TABLE orders (" +
                        "    o_orderkey    BIGINT," +
                        "    o_custkey     BIGINT," +
                        "    o_orderstatus STRING," +
                        "    o_totalprice  DECIMAL(15, 2)," +
                        "    o_orderdate   DATE," +
                        "    o_orderpriority STRING," +
                        "    o_clerk        STRING," +
                        "    o_shippriority INTEGER," +
                        "    o_comment      STRING," +
                        "    proctime AS PROCTIME()" +
                        ") WITH (" +
                        "    'connector' = 'datagen'," +
                        "    'rows-per-second' = '50'," +
                        "    'fields.o_orderkey.min' = '1'," +
                        "    'fields.o_orderkey.max' = '1000'," +
                        "    'properties.group.id' = 'tpch-query-group'," +
                        "    'properties.auto.offset.reset' = 'earliest'" +
                        ")"
        );
    }
}