package com.example.bff.config;

import com.example.bff.websocket.FlutterWebSocketHandler;
import com.example.bff.websocket.GatewayWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Autowired
    private FlutterWebSocketHandler flutterWebSocketHandler;

    @Autowired
    private GatewayWebSocketHandler gatewayWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Handler for Flutter clients - requires authentication via first message
        registry.addHandler(flutterWebSocketHandler, "/vx/flutter")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        attributes.put("clientType", "flutter");
                        attributes.put("authenticated", false);
                        logger.info("BFF Flutter WebSocket handshake - URI: {}, Address: {}", 
                                request.getURI(), request.getRemoteAddress());
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Exception exception) {
                        if (exception != null) {
                            logger.error("BFF Flutter WebSocket Handshake failed: {}", exception.getMessage());
                        } else {
                            logger.info("✓ BFF Flutter WebSocket Handshake completed successfully");
                        }
                    }
                });

        // Handler for OrangePi Gateway devices - proxies to router-backend
        registry.addHandler(gatewayWebSocketHandler, "/vx/gateway")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        attributes.put("clientType", "orangepi_gateway");
                        logger.info("BFF Gateway WebSocket handshake - URI: {}, Address: {}", 
                                request.getURI(), request.getRemoteAddress());
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Exception exception) {
                        if (exception != null) {
                            logger.error("BFF Gateway WebSocket Handshake failed: {}", exception.getMessage());
                        } else {
                            logger.info("✓ BFF Gateway WebSocket Handshake completed successfully");
                        }
                    }
                });

        // Handler for OrangePi Device Discovery - proxies to router-backend
        registry.addHandler(gatewayWebSocketHandler, "/vx/gateway/deviceDiscovery")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        attributes.put("clientType", "orangepi_device_discovery");
                        logger.info("BFF Device Discovery WebSocket handshake - URI: {}, Address: {}", 
                                request.getURI(), request.getRemoteAddress());
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Exception exception) {
                        if (exception != null) {
                            logger.error("BFF Device Discovery WebSocket Handshake failed: {}", exception.getMessage());
                        } else {
                            logger.info("✓ BFF Device Discovery WebSocket Handshake completed successfully");
                        }
                    }
                });

        // Handler for OrangePi Device Assignment - proxies to WebSocket Service
        registry.addHandler(gatewayWebSocketHandler, "/vx/device-assignment")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        attributes.put("clientType", "orangepi_device_assignment");
                        logger.info("BFF Device Assignment WebSocket handshake - URI: {}, Address: {}", 
                                request.getURI(), request.getRemoteAddress());
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Exception exception) {
                        if (exception != null) {
                            logger.error("BFF Device Assignment WebSocket Handshake failed: {}", exception.getMessage());
                        } else {
                            logger.info("✓ BFF Device Assignment WebSocket Handshake completed successfully");
                        }
                    }
                });

        // Handler for OrangePi eBPF stats - proxies to WebSocket Service
        registry.addHandler(gatewayWebSocketHandler, "/vx/gateway/ebpfstats")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        attributes.put("clientType", "orangepi_ebpf_stats");
                        logger.info("BFF eBPF Stats WebSocket handshake - URI: {}, Address: {}",
                                request.getURI(), request.getRemoteAddress());
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                            WebSocketHandler wsHandler, Exception exception) {
                        if (exception != null) {
                            logger.error("BFF eBPF Stats WebSocket Handshake failed: {}", exception.getMessage());
                        } else {
                            logger.info("✓ BFF eBPF Stats WebSocket Handshake completed successfully");
                        }
                    }
                });

        logger.info("BFF WebSocket endpoints registered: /vx/flutter, /vx/gateway, /vx/gateway/deviceDiscovery, /vx/device-assignment, /vx/gateway/ebpfstats");
    }
}
