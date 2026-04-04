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
package com.getboot.websocket.api.sender;

/**
 * WebSocket 消息推送门面。
 *
 * @author qiheng
 */
public interface WebSocketMessageSender {

    /**
     * 按会话 ID 推送消息。
     *
     * @param sessionId 会话 ID
     * @param payload 消息内容
     * @return 成功进入发送流程的会话数
     */
    int sendToSession(String sessionId, Object payload);

    /**
     * 按用户标识推送消息。
     *
     * @param userId 用户标识
     * @param payload 消息内容
     * @return 成功进入发送流程的会话数
     */
    int sendToUser(String userId, Object payload);

    /**
     * 广播消息。
     *
     * @param payload 消息内容
     * @return 成功进入发送流程的会话数
     */
    int broadcast(Object payload);
}
