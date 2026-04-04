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
package com.getboot.websocket.api.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket 模块配置。
 *
 * @author qiheng
 */
@Data
@ConfigurationProperties(prefix = "getboot.websocket")
public class WebSocketProperties {

    /**
     * 是否启用 WebSocket 能力模块。
     */
    private boolean enabled = true;

    /**
     * 默认 Endpoint 路径。
     */
    private String endpoint = "/getboot/ws";

    /**
     * 允许的 Origin 列表。
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of("*"));

    /**
     * 握手请求中读取 TraceId 的头名称。
     */
    private String traceHeaderName = "X-Trace-Id";

    /**
     * 握手请求中读取用户标识的头名称。
     */
    private String userIdHeaderName = "X-User-Id";

    /**
     * 握手请求中读取用户标识的查询参数名称。
     */
    private String userIdQueryParameter = "userId";

    /**
     * 异步发送超时时间，单位毫秒。
     */
    private long asyncSendTimeoutMs = 10_000L;
}
