package com.example.model;

import lombok.Data;
import java.util.Map;

@Data
public class QueryResult {
    private String queryId;
    private String queryName;
    private boolean error;
    private String message;
    private Map<String, Object> data;
}