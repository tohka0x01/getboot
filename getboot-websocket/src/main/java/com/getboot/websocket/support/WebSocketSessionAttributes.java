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
package com.getboot.websocket.support;

/**
 * WebSocket 会话属性键集合。
 *
 * @author qiheng
 */
public final class WebSocketSessionAttributes {

    /**
     * 当前连接绑定的用户标识键。
     */
    public static final String USER_ID = "getboot.websocket.userId";

    /**
     * 当前连接绑定的 TraceId 键。
     */
    public static final String TRACE_ID = "getboot.websocket.traceId";

    private WebSocketSessionAttributes() {
    }
}
