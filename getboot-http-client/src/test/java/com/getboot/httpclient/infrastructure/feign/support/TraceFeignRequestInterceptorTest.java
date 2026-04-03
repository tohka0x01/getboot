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
package com.getboot.httpclient.infrastructure.feign.support;

import com.getboot.httpclient.api.properties.OpenFeignTraceProperties;
import com.getboot.httpclient.spi.OutboundHttpHeadersContributor;
import com.getboot.httpclient.spi.feign.OpenFeignTraceRequestCustomizer;
import com.getboot.httpclient.support.headers.OutboundHttpHeadersResolver;
import com.getboot.support.api.trace.TraceContextHolder;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link TraceFeignRequestInterceptor} 测试。
 *
 * @author qiheng
 */
class TraceFeignRequestInterceptorTest {

    /**
     * 验证没有 TraceId 时仍会应用通用出站请求头。
     */
    @Test
    void shouldApplyCommonHeadersWithoutTraceId() {
        TraceFeignRequestInterceptor interceptor = new TraceFeignRequestInterceptor(
                new OpenFeignTraceProperties(),
                new OutboundHttpHeadersResolver(List.of((headers, context) -> headers.add("X-Tenant-Id", "tenant-001"))),
                List.of()
        );
        RequestTemplate requestTemplate = new RequestTemplate();

        interceptor.apply(requestTemplate);

        assertEquals(List.of("tenant-001"), List.copyOf(requestTemplate.headers().get("X-Tenant-Id")));
        assertFalse(requestTemplate.headers().containsKey("X-Trace-Id"));
    }

    /**
     * 验证存在 TraceId 时会同时应用 Trace 请求头、通用请求头和 Feign 定制逻辑。
     */
    @Test
    void shouldApplyTraceHeaderCommonHeadersAndFeignCustomizer() {
        String previousTraceId = TraceContextHolder.bindTraceId("trace-002");
        try {
            OutboundHttpHeadersContributor contributor = (headers, context) -> headers.add("X-App-Id", "demo-service");
            OpenFeignTraceRequestCustomizer customizer =
                    (requestTemplate, traceId, traceHeaderName) -> requestTemplate.header("X-Trace-Suffix", traceId + "-suffix");
            TraceFeignRequestInterceptor interceptor = new TraceFeignRequestInterceptor(
                    new OpenFeignTraceProperties(),
                    new OutboundHttpHeadersResolver(List.of(contributor)),
                    List.of(customizer)
            );
            RequestTemplate requestTemplate = new RequestTemplate();

            interceptor.apply(requestTemplate);

            assertEquals(List.of("trace-002"), List.copyOf(requestTemplate.headers().get("X-Trace-Id")));
            assertEquals(List.of("demo-service"), List.copyOf(requestTemplate.headers().get("X-App-Id")));
            assertEquals(List.of("trace-002-suffix"), List.copyOf(requestTemplate.headers().get("X-Trace-Suffix")));
        } finally {
            TraceContextHolder.restoreTraceId(previousTraceId);
        }
    }
}
