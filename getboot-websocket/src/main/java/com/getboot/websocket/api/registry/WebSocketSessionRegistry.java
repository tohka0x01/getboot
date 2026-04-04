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
package com.getboot.websocket.api.registry;

import jakarta.websocket.Session;

import java.util.Collection;
import java.util.Optional;

/**
 * WebSocket 会话注册表。
 *
 * @author qiheng
 */
public interface WebSocketSessionRegistry {

    /**
     * 注册会话。
     *
     * @param session WebSocket 会话
     */
    void register(Session session);

    /**
     * 注销会话。
     *
     * @param sessionId 会话 ID
     */
    void unregister(String sessionId);

    /**
     * 获取指定会话。
     *
     * @param sessionId 会话 ID
     * @return 会话
     */
    Optional<Session> get(String sessionId);

    /**
     * 获取指定用户的全部会话。
     *
     * @param userId 用户标识
     * @return 会话集合
     */
    Collection<Session> getByUserId(String userId);

    /**
     * 获取当前全部已注册会话。
     *
     * @return 会话集合
     */
    Collection<Session> getAll();
}
