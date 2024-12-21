package com.example.config;

import com.example.handler.LiveQueryWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import java.util.Properties;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LiveQueryWebSocketHandler webSocketHandler;
    private final Properties kafkaProperties;

    public WebSocketConfig() {
        this.kafkaProperties = new Properties();
        kafkaProperties.setProperty("bootstrap.servers", "localhost:9092");
        kafkaProperties.setProperty("group.id", "tpch-query-group");
        kafkaProperties.setProperty("auto.offset.reset", "earliest");

        this.webSocketHandler = new LiveQueryWebSocketHandler(kafkaProperties);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/query")
                .setAllowedOrigins("*");
    }

    @Bean
    public Properties kafkaProperties() {
        return kafkaProperties;
    }

    @Bean
    public LiveQueryWebSocketHandler liveQueryWebSocketHandler() {
        return webSocketHandler;
    }
}