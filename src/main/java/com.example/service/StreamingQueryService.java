package com.example.service;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.springframework.stereotype.Service;
import com.example.model.QueryResult;
import com.example.service.sink.QueryResultSink;
import org.apache.flink.table.api.EnvironmentSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
public class StreamingQueryService {
    private final StreamExecutionEnvironment env;
    private final StreamTableEnvironment tableEnv;
    private final Map<String, JobInfo> runningJobs;
    private final Map<String, String[]> queryFieldNames;
    private Properties kafkaProperties;

    public StreamingQueryService() {
        this(getDefaultKafkaProperties());
    }

    public StreamingQueryService(Properties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;

        this.env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .build();
        this.tableEnv = StreamTableEnvironment.create(env, settings);

        this.runningJobs = new ConcurrentHashMap<>();
        this.queryFieldNames = new HashMap<>();

        initializeQueryFieldNames();
        registerTPCHTables();
    }

    private void initializeQueryFieldNames() {
        queryFieldNames.put("Q1", new String[]{
                "l_returnflag",
                "l_linestatus",
                "sum_qty",
                "sum_base_price",
                "sum_disc_price",
                "sum_charge",
                "avg_qty",
                "avg_price",
                "avg_disc",
                "count_order"
        });

        queryFieldNames.put("Q3", new String[]{
                "l_orderkey",
                "revenue",
                "o_orderdate",
                "o_shippriority"
        });
    }

    private static Properties getDefaultKafkaProperties() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost:9092");
        props.setProperty("group.id", "tpch-query-group");
        props.setProperty("auto.offset.reset", "earliest");
        return props;
    }

    public void setKafkaProperties(Properties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
        registerTPCHTables();
    }

    private void registerTPCHTables() {
        tableEnv.executeSql("DROP TABLE IF EXISTS lineitem");
        tableEnv.executeSql("DROP TABLE IF EXISTS orders");
        tableEnv.executeSql("DROP TABLE IF EXISTS customer");

        tableEnv.executeSql(String.format("""
            CREATE TABLE lineitem (
                l_orderkey      BIGINT,
                l_partkey       BIGINT,
                l_suppkey       BIGINT,
                l_linenumber    INT,
                l_quantity      DECIMAL(15, 2),
                l_extendedprice DECIMAL(15, 2),
                l_discount      DECIMAL(15, 2),
                l_tax           DECIMAL(15, 2),
                l_returnflag    CHAR(1),
                l_linestatus    CHAR(1),
                l_shipdate      DATE,
                l_commitdate    DATE,
                l_receiptdate   DATE,
                l_shipinstruct  VARCHAR(25),
                l_shipmode      VARCHAR(10),
                l_comment       VARCHAR(44)
            ) WITH (
                'connector' = 'kafka',
                'topic' = 'lineitem',
                'properties.bootstrap.servers' = '%s',
                'properties.group.id' = '%s',
                'scan.startup.mode' = 'earliest-offset',
                'format' = 'json',
                'json.fail-on-missing-field' = 'false',
                'json.ignore-parse-errors' = 'true'
            )
        """, kafkaProperties.getProperty("bootstrap.servers"),
                kafkaProperties.getProperty("group.id")));

        tableEnv.executeSql(String.format("""
            CREATE TABLE orders (
                o_orderkey       BIGINT,
                o_custkey       BIGINT,
                o_orderstatus   CHAR(1),
                o_totalprice    DECIMAL(15, 2),
                o_orderdate     DATE,
                o_orderpriority VARCHAR(15),
                o_clerk         VARCHAR(15),
                o_shippriority  INT,
                o_comment       VARCHAR(79)
            ) WITH (
                'connector' = 'kafka',
                'topic' = 'orders',
                'properties.bootstrap.servers' = '%s',
                'properties.group.id' = '%s',
                'scan.startup.mode' = 'earliest-offset',
                'format' = 'json',
                'json.fail-on-missing-field' = 'false',
                'json.ignore-parse-errors' = 'true'
            )
        """, kafkaProperties.getProperty("bootstrap.servers"),
                kafkaProperties.getProperty("group.id")));

        tableEnv.executeSql(String.format("""
            CREATE TABLE customer (
                c_custkey       BIGINT,
                c_name          VARCHAR(25),
                c_address       VARCHAR(40),
                c_nationkey     BIGINT,
                c_phone         VARCHAR(15),
                c_acctbal       DECIMAL(15, 2),
                c_mktsegment    VARCHAR(10),
                c_comment       VARCHAR(117)
            ) WITH (
                'connector' = 'kafka',
                'topic' = 'customer',
                'properties.bootstrap.servers' = '%s',
                'properties.group.id' = '%s',
                'scan.startup.mode' = 'earliest-offset',
                'format' = 'json',
                'json.fail-on-missing-field' = 'false',
                'json.ignore-parse-errors' = 'true'
            )
        """, kafkaProperties.getProperty("bootstrap.servers"),
                kafkaProperties.getProperty("group.id")));
    }

    public String startQuery(String queryName, Consumer<QueryResult> resultHandler) {
        String queryId = UUID.randomUUID().toString();

        try {
            Table resultTable = tableEnv.sqlQuery(getQuerySQL(queryName));
            DataStream<Row> resultStream = tableEnv.toDataStream(resultTable);

            resultStream.map(row -> {
                QueryResult result = new QueryResult();
                result.setQueryId(queryId);
                result.setQueryName(queryName);
                result.setData(convertRowToMap(row, queryName));
                return result;
            }).addSink(new QueryResultSink(resultHandler));

            Thread jobThread = new Thread(() -> {
                try {
                    env.execute("Query-" + queryId);
                } catch (Exception e) {
                    QueryResult errorResult = new QueryResult();
                    errorResult.setQueryId(queryId);
                    errorResult.setError(true);
                    errorResult.setMessage(e.getMessage());
                    resultHandler.accept(errorResult);
                }
            });

            JobInfo jobInfo = new JobInfo(jobThread, env.getExecutionPlan());
            runningJobs.put(queryId, jobInfo);
            jobThread.start();

            return queryId;
        } catch (Exception e) {
            QueryResult errorResult = new QueryResult();
            errorResult.setQueryId(queryId);
            errorResult.setError(true);
            errorResult.setMessage(e.getMessage());
            resultHandler.accept(errorResult);
            return null;
        }
    }

    public void stopQuery(String queryId) {
        JobInfo jobInfo = runningJobs.remove(queryId);
        if (jobInfo != null && jobInfo.getThread().isAlive()) {
            jobInfo.getThread().interrupt();
        }
    }

    private Map<String, Object> convertRowToMap(Row row, String queryName) {
        Map<String, Object> result = new HashMap<>();
        String[] fields = queryFieldNames.get(queryName);

        if (fields == null) {
            for (int i = 0; i < row.getArity(); i++) {
                result.put("column_" + i, row.getField(i));
            }
        } else {
            for (int i = 0; i < fields.length; i++) {
                result.put(fields[i], row.getField(i));
            }
        }
        return result;
    }

    private String getQuerySQL(String queryName) {
        return switch (queryName) {
            case "Q1" -> """
                SELECT
                    l_returnflag,
                    l_linestatus,
                    SUM(l_quantity) as sum_qty,
                    SUM(l_extendedprice) as sum_base_price,
                    SUM(l_extendedprice * (1 - l_discount)) as sum_disc_price,
                    SUM(l_extendedprice * (1 - l_discount) * (1 + l_tax)) as sum_charge,
                    AVG(l_quantity) as avg_qty,
                    AVG(l_extendedprice) as avg_price,
                    AVG(l_discount) as avg_disc,
                    COUNT(*) as count_order
                FROM lineitem
                GROUP BY l_returnflag, l_linestatus
                """;
            case "Q3" -> """
                SELECT
                    l.l_orderkey,
                    SUM(l.l_extendedprice * (1 - l.l_discount)) as revenue,
                    o.o_orderdate,
                    o.o_shippriority
                FROM
                    customer c,
                    orders o,
                    lineitem l
                WHERE
                    c.c_mktsegment = 'BUILDING'
                    AND c.c_custkey = o.o_custkey
                    AND l.l_orderkey = o.o_orderkey
                GROUP BY
                    l.l_orderkey,
                    o.o_orderdate,
                    o.o_shippriority
                ORDER BY
                    revenue DESC,
                    o.o_orderdate
                LIMIT 10
                """;
            default -> throw new IllegalArgumentException("Unknown query: " + queryName);
        };
    }

    private static class JobInfo {
        private final Thread thread;
        private final String executionPlan;

        public JobInfo(Thread thread, String executionPlan) {
            this.thread = thread;
            this.executionPlan = executionPlan;
        }

        public Thread getThread() {
            return thread;
        }

        public String getExecutionPlan() {
            return executionPlan;
        }
    }
}