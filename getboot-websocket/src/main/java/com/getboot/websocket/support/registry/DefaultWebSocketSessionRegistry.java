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
package com.getboot.websocket.support.registry;

import com.getboot.websocket.api.registry.WebSocketSessionRegistry;
import com.getboot.websocket.support.WebSocketSessionAttributes;
import jakarta.websocket.Session;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 WebSocket 会话注册表。
 *
 * @author qiheng
 */
public class DefaultWebSocketSessionRegistry implements WebSocketSessionRegistry {

    /**
     * 按会话 ID 维护会话。
     */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * 按用户标识维护会话 ID 集合。
     */
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    /**
     * 注册会话。
     *
     * @param session WebSocket 会话
     */
    @Override
    public void register(Session session) {
        if (session == null) {
            return;
        }
        sessions.put(session.getId(), session);
        Object userId = session.getUserProperties().get(WebSocketSessionAttributes.USER_ID);
        if (userId instanceof String text && StringUtils.hasText(text)) {
            userSessions.computeIfAbsent(text.trim(), ignored -> ConcurrentHashMap.newKeySet()).add(session.getId());
        }
    }

    /**
     * 注销会话。
     *
     * @param sessionId 会话 ID
     */
    @Override
    public void unregister(String sessionId) {
        Session session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        Object userId = session.getUserProperties().get(WebSocketSessionAttributes.USER_ID);
        if (userId instanceof String text && StringUtils.hasText(text)) {
            Set<String> sessionIds = userSessions.get(text.trim());
            if (sessionIds == null) {
                return;
            }
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                userSessions.remove(text.trim());
            }
        }
    }

    /**
     * 获取指定会话。
     *
     * @param sessionId 会话 ID
     * @return 会话
     */
    @Override
    public Optional<Session> get(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null || !session.isOpen()) {
            return Optional.empty();
        }
        return Optional.of(session);
    }

    /**
     * 获取指定用户的全部会话。
     *
     * @param userId 用户标识
     * @return 会话集合
     */
    @Override
    public Collection<Session> getByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return List.of();
        }
        Set<String> sessionIds = userSessions.get(userId.trim());
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        return sessionIds.stream()
                .map(sessions::get)
                .filter(session -> session != null && session.isOpen())
                .toList();
    }

    /**
     * 获取当前全部已注册会话。
     *
     * @return 会话集合
     */
    @Override
    public Collection<Session> getAll() {
        return sessions.values().stream()
                .filter(Session::isOpen)
                .toList();
    }
}
