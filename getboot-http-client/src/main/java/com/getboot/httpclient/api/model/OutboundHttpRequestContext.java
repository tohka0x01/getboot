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
package com.getboot.httpclient.api.model;

import org.springframework.util.StringUtils;

/**
 * 出站 HTTP 请求上下文。
 *
 * <p>用于模块通用出站头扩展点感知当前客户端类型与 Trace 透传信息。</p>
 *
 * @author qiheng
 */
public class OutboundHttpRequestContext {

    private final OutboundHttpClientType clientType;
    private final String traceId;
    private final String traceHeaderName;

    public OutboundHttpRequestContext(OutboundHttpClientType clientType,
                                      String traceId,
                                      String traceHeaderName) {
        this.clientType = clientType;
        this.traceId = traceId;
        this.traceHeaderName = traceHeaderName;
    }

    public OutboundHttpClientType getClientType() {
        return clientType;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getTraceHeaderName() {
        return traceHeaderName;
    }

    public boolean hasTraceId() {
        return StringUtils.hasText(traceId);
    }
}
