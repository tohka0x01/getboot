/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.getboot.websocket.infrastructure.jakarta.endpoint;

import com.getboot.support.api.trace.TraceContextHolder;
import com.getboot.websocket.api.properties.WebSocketProperties;
import com.getboot.websocket.api.registry.WebSocketSessionRegistry;
import com.getboot.websocket.spi.WebSocketSessionLifecycleListener;
import com.getboot.websocket.spi.WebSocketTextMessageListener;
import com.getboot.websocket.spi.WebSocketUserIdResolver;
import com.getboot.websocket.support.WebSocketSessionAttributes;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 默认 WebSocket Endpoint。
 *
 * @author qiheng
 */
public class GetbootWebSocketEndpoint extends Endpoint {

    /**
     * 会话注册表。
     */
    private final WebSocketSessionRegistry sessionRegistry;

    /**
     * WebSocket 模块配置。
     */
    private final WebSocketProperties properties;

    /**
     * 用户标识解析器。
     */
    private final WebSocketUserIdResolver userIdResolver;

    /**
     * 生命周期监听器集合。
     */
    private final List<WebSocketSessionLifecycleListener> lifecycleListeners;

    /**
     * 文本消息监听器集合。
     */
    private final List<WebSocketTextMessageListener> textMessageListeners;

    /**
     * 创建默认 Endpoint。
     *
     * @param sessionRegistry 会话注册表
     * @param properties WebSocket 模块配置
     * @param userIdResolver 用户标识解析器
     * @param lifecycleListeners 生命周期监听器集合
     * @param textMessageListeners 文本消息监听器集合
     */
    public GetbootWebSocketEndpoint(
            WebSocketSessionRegistry sessionRegistry,
            WebSocketProperties properties,
            WebSocketUserIdResolver userIdResolver,
            List<WebSocketSessionLifecycleListener> lifecycleListeners,
            List<WebSocketTextMessageListener> textMessageListeners) {
        this.sessionRegistry = sessionRegistry;
        this.properties = properties;
        this.userIdResolver = userIdResolver;
        this.lifecycleListeners = lifecycleListeners == null ? List.of() : List.copyOf(lifecycleListeners);
        this.textMessageListeners = textMessageListeners == null ? List.of() : List.copyOf(textMessageListeners);
    }

    /**
     * 处理连接建立。
     *
     * @param session 当前会话
     * @param endpointConfig Endpoint 配置
     */
    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        session.getAsyncRemote().setSendTimeout(properties.getAsyncSendTimeoutMs());
        Object traceId = endpointConfig.getUserProperties().get(WebSocketSessionAttributes.TRACE_ID);
        if (traceId instanceof String text && StringUtils.hasText(text)) {
            session.getUserProperties().put(WebSocketSessionAttributes.TRACE_ID, text.trim());
        }
        String userId = userIdResolver.resolve(session, endpointConfig);
        if (StringUtils.hasText(userId)) {
            session.getUserProperties().put(WebSocketSessionAttributes.USER_ID, userId.trim());
        }
        sessionRegistry.register(session);
        session.addMessageHandler((MessageHandler.Whole<String>) message -> handleTextMessage(session, message));
        lifecycleListeners.forEach(listener -> listener.afterConnected(session));
    }

    /**
     * 处理连接关闭。
     *
     * @param session 当前会话
     * @param closeReason 关闭原因
     */
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        sessionRegistry.unregister(session.getId());
        lifecycleListeners.forEach(listener -> listener.afterDisconnected(session, closeReason));
    }

    /**
     * 处理连接异常。
     *
     * @param session 当前会话
     * @param throwable 异常
     */
    @Override
    public void onError(Session session, Throwable throwable) {
        if (session != null && !session.isOpen()) {
            sessionRegistry.unregister(session.getId());
        }
    }

    /**
     * 处理文本消息。
     *
     * @param session 当前会话
     * @param message 文本消息
     */
    private void handleTextMessage(Session session, String message) {
        Object traceId = session.getUserProperties().get(WebSocketSessionAttributes.TRACE_ID);
        String previousTraceId = null;
        if (traceId instanceof String text && StringUtils.hasText(text)) {
            previousTraceId = TraceContextHolder.bindTraceId(text.trim());
        }
        try {
            for (WebSocketTextMessageListener listener : textMessageListeners) {
                listener.handle(session, message);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to handle WebSocket text message.", ex);
        } finally {
            TraceContextHolder.restoreTraceId(previousTraceId);
        }
    }
}
