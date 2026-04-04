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
package com.getboot.websocket.support.sender;

import com.alibaba.fastjson2.JSON;
import com.getboot.websocket.api.registry.WebSocketSessionRegistry;
import com.getboot.websocket.api.sender.WebSocketMessageSender;
import jakarta.websocket.Session;

import java.util.Collection;

/**
 * 默认 WebSocket 消息推送器。
 *
 * @author qiheng
 */
public class DefaultWebSocketMessageSender implements WebSocketMessageSender {

    /**
     * WebSocket 会话注册表。
     */
    private final WebSocketSessionRegistry sessionRegistry;

    /**
     * 创建默认 WebSocket 消息推送器。
     *
     * @param sessionRegistry 会话注册表
     */
    public DefaultWebSocketMessageSender(WebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * 按会话 ID 推送消息。
     *
     * @param sessionId 会话 ID
     * @param payload 消息内容
     * @return 成功进入发送流程的会话数
     */
    @Override
    public int sendToSession(String sessionId, Object payload) {
        return sessionRegistry.get(sessionId)
                .map(session -> send(session, payload))
                .orElse(0);
    }

    /**
     * 按用户标识推送消息。
     *
     * @param userId 用户标识
     * @param payload 消息内容
     * @return 成功进入发送流程的会话数
     */
    @Override
    public int sendToUser(String userId, Object payload) {
        return sendAll(sessionRegistry.getByUserId(userId), payload);
    }

    /**
     * 广播消息。
     *
     * @param payload 消息内容
     * @return 成功进入发送流程的会话数
     */
    @Override
    public int broadcast(Object payload) {
        return sendAll(sessionRegistry.getAll(), payload);
    }

    /**
     * 批量发送消息。
     *
     * @param sessions 会话集合
     * @param payload 消息内容
     * @return 成功进入发送流程的会话数
     */
    private int sendAll(Collection<Session> sessions, Object payload) {
        int successCount = 0;
        for (Session session : sessions) {
            successCount += send(session, payload);
        }
        return successCount;
    }

    /**
     * 向单个会话发送消息。
     *
     * @param session WebSocket 会话
     * @param payload 消息内容
     * @return 成功进入发送流程时返回 1
     */
    private int send(Session session, Object payload) {
        if (session == null || !session.isOpen()) {
            return 0;
        }
        session.getAsyncRemote().sendText(toTextPayload(payload));
        return 1;
    }

    /**
     * 将对象转换为文本消息。
     *
     * @param payload 消息内容
     * @return 文本消息
     */
    private String toTextPayload(Object payload) {
        if (payload instanceof String text) {
            return text;
        }
        return JSON.toJSONString(payload);
    }
}
