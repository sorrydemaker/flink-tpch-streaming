package com.example.handler;

import com.example.model.QueryRequest;
import com.example.model.QueryResult;
import com.example.service.StreamingQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Properties;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LiveQueryWebSocketHandler extends TextWebSocketHandler {

    private final StreamingQueryService queryService;
    private final ObjectMapper objectMapper;
    private final Properties kafkaProperties;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionQueries = new ConcurrentHashMap<>();

    @Autowired
    public LiveQueryWebSocketHandler(StreamingQueryService queryService, ObjectMapper objectMapper) {
        this.queryService = queryService;
        this.objectMapper = objectMapper;
        this.kafkaProperties = new Properties();
        this.kafkaProperties.setProperty("bootstrap.servers", "localhost:9092");
        this.kafkaProperties.setProperty("group.id", "tpch-query-group");
        this.kafkaProperties.setProperty("auto.offset.reset", "earliest");
    }

    public LiveQueryWebSocketHandler(Properties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
        this.objectMapper = new ObjectMapper();
        this.queryService = new StreamingQueryService(kafkaProperties);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket connection established: {}", session.getId());

        try {
            QueryResult connectResult = new QueryResult();
            connectResult.setMessage("Connected");
            sendQueryResult(session, connectResult);
        } catch (Exception e) {
            log.error("Error sending connection confirmation", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            QueryRequest request = objectMapper.readValue(message.getPayload(), QueryRequest.class);
            log.info("Received request: {}", request);

            request.setKafkaProperties(kafkaProperties);

            switch (request.getAction()) {
                case "start" -> {
                    String queryId = queryService.startQuery(
                            request.getQueryName(),
                            result -> sendQueryResult(session, result)
                    );
                    if (queryId != null) {
                        sessionQueries.put(session.getId(), queryId);
                        log.info("Started query {} for session {}", queryId, session.getId());
                    } else {
                        sendError(session, "Failed to start query");
                    }
                }
                case "stop" -> {
                    String queryId = sessionQueries.get(session.getId());
                    if (queryId != null) {
                        queryService.stopQuery(queryId);
                        sessionQueries.remove(session.getId());
                        log.info("Stopped query {} for session {}", queryId, session.getId());
                    } else {
                        sendError(session, "No active query found");
                    }
                }
                default -> sendError(session, "Unknown action: " + request.getAction());
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
            sendError(session, "Error processing request: " + e.getMessage());
        }
    }

    private void sendQueryResult(WebSocketSession session, QueryResult result) {
        try {
            if (session.isOpen()) {
                String json = objectMapper.writeValueAsString(result);
                session.sendMessage(new TextMessage(json));
                log.debug("Sent query result to session {}: {}", session.getId(), json);
            }
        } catch (Exception e) {
            log.error("Error sending query result to session {}", session.getId(), e);
        }
    }

    private void sendError(WebSocketSession session, String error) {
        try {
            if (session.isOpen()) {
                QueryResult errorResult = new QueryResult();
                errorResult.setError(true);
                errorResult.setMessage(error);
                String json = objectMapper.writeValueAsString(errorResult);
                session.sendMessage(new TextMessage(json));
                log.error("Sent error to session {}: {}", session.getId(), error);
            }
        } catch (Exception e) {
            log.error("Error sending error message to session {}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        String queryId = sessionQueries.remove(session.getId());
        if (queryId != null) {
            queryService.stopQuery(queryId);
            log.info("Stopped query {} after session {} closed", queryId, session.getId());
        }
        sessions.remove(session.getId());
        log.info("WebSocket connection closed: {} with status {}", session.getId(), status);
    }

    // Getter for Kafka properties
    public Properties getKafkaProperties() {
        return kafkaProperties;
    }
}