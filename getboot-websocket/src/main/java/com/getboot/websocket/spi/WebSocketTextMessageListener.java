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
package com.getboot.websocket.spi;

import jakarta.websocket.Session;

/**
 * WebSocket 文本消息监听器。
 *
 * @author qiheng
 */
@FunctionalInterface
public interface WebSocketTextMessageListener {

    /**
     * 处理文本消息。
     *
     * @param session 当前会话
     * @param message 文本消息
     * @throws Exception 处理异常
     */
    void handle(Session session, String message) throws Exception;
}
