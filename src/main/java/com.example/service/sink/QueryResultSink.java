package com.example.service.sink;

import com.example.model.QueryResult;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import java.util.function.Consumer;

public class QueryResultSink extends RichSinkFunction<QueryResult> {

    private final Consumer<QueryResult> resultHandler;

    public QueryResultSink(Consumer<QueryResult> resultHandler) {
        this.resultHandler = resultHandler;
    }

    @Override
    public void invoke(QueryResult result, Context context) {
        resultHandler.accept(result);
    }
}