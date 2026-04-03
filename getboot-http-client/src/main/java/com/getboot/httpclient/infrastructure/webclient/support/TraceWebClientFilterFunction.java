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
package com.getboot.httpclient.infrastructure.webclient.support;

import com.getboot.httpclient.api.model.OutboundHttpClientType;
import com.getboot.httpclient.api.model.OutboundHttpRequestContext;
import com.getboot.httpclient.api.properties.WebClientTraceProperties;
import com.getboot.httpclient.spi.webclient.WebClientTraceRequestCustomizer;
import com.getboot.httpclient.support.headers.OutboundHttpHeadersResolver;
import com.getboot.support.api.trace.TraceContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * WebClient Trace 过滤器。
 *
 * <p>用于将当前线程中的 TraceId 自动写入 WebClient 出站请求头。</p>
 *
 * @author qiheng
 */
public class TraceWebClientFilterFunction implements ExchangeFilterFunction {

    /**
     * WebClient Trace 配置。
     */
    private final WebClientTraceProperties properties;

    /**
     * 出站请求头解析器。
     */
    private final OutboundHttpHeadersResolver outboundHttpHeadersResolver;

    /**
     * WebClient 请求定制器集合。
     */
    private final List<WebClientTraceRequestCustomizer> customizers;

    /**
     * 创建 WebClient Trace 过滤器。
     *
     * @param properties WebClient Trace 配置
     * @param outboundHttpHeadersResolver 出站请求头解析器
     * @param customizers WebClient 请求定制器集合
     */
    public TraceWebClientFilterFunction(
            WebClientTraceProperties properties,
            OutboundHttpHeadersResolver outboundHttpHeadersResolver,
            List<WebClientTraceRequestCustomizer> customizers) {
        this.properties = properties;
        this.outboundHttpHeadersResolver = outboundHttpHeadersResolver;
        this.customizers = customizers == null ? List.of() : List.copyOf(customizers);
    }

    /**
     * 将公共请求头和 Trace 请求头写入 WebClient 请求。
     *
     * @param request 当前请求
     * @param next 下游交换函数
     * @return 响应流
     */
    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        String traceId = TraceContextHolder.getTraceId();
        String traceHeaderName = properties.getHeaderName();
        HttpHeaders resolvedHeaders = outboundHttpHeadersResolver.resolve(
                new OutboundHttpRequestContext(OutboundHttpClientType.WEB_CLIENT, traceId, traceHeaderName)
        );
        if (resolvedHeaders.isEmpty() && !StringUtils.hasText(traceId)) {
            return next.exchange(request);
        }
        ClientRequest.Builder requestBuilder = ClientRequest.from(request);
        requestBuilder.headers(headers -> {
            resolvedHeaders.forEach((headerName, values) -> {
                headers.remove(headerName);
                headers.addAll(headerName, values);
            });
            if (StringUtils.hasText(traceId)) {
                customizers.forEach(customizer -> customizer.customize(headers, request, traceId, traceHeaderName));
            }
        });
        return next.exchange(requestBuilder.build());
    }
}
