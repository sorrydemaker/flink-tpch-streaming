package com.example.model;

import lombok.Data;
import java.util.Properties;

@Data
public class QueryRequest {
    private String action;
    private String queryName;
    private String queryId;
    private Properties kafkaProperties;

    public QueryRequest() {
    }

    public QueryRequest(String action, String queryName) {
        this.action = action;
        this.queryName = queryName;
    }

    public QueryRequest(String action, String queryName, String queryId, Properties kafkaProperties) {
        this.action = action;
        this.queryName = queryName;
        this.queryId = queryId;
        this.kafkaProperties = kafkaProperties;
    }

    public void setDefaultKafkaProperties() {
        if (this.kafkaProperties == null) {
            this.kafkaProperties = new Properties();
            this.kafkaProperties.setProperty("bootstrap.servers", "localhost:9092");
            this.kafkaProperties.setProperty("group.id", "tpch-query-group");
            this.kafkaProperties.setProperty("auto.offset.reset", "earliest");
        }
    }

    public boolean isValid() {
        if (action == null || action.trim().isEmpty()) {
            return false;
        }

        if ("start".equals(action) && (queryName == null || queryName.trim().isEmpty())) {
            return false;
        }

        if ("stop".equals(action) && (queryId == null || queryId.trim().isEmpty())) {
            return false;
        }

        return true;
    }

    public String getActionDescription() {
        return switch (action) {
            case "start" -> "Start Query";
            case "stop" -> "Stop query";
            default -> "Unknown operation";
        };
    }

    @Override
    public String toString() {
        return "QueryRequest{" +
                "action='" + action + '\'' +
                ", queryName='" + queryName + '\'' +
                ", queryId='" + queryId + '\'' +
                ", kafkaProperties=" + (kafkaProperties != null ? "configured" : "not configured") +
                '}';
    }
}