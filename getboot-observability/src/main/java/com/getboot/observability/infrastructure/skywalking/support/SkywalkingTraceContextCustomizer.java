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
package com.getboot.observability.infrastructure.skywalking.support;

import com.getboot.observability.api.context.TraceContext;
import com.getboot.observability.api.properties.ObservabilitySkywalkingProperties;
import com.getboot.observability.api.context.ReactiveTraceContext;
import com.getboot.observability.spi.ReactiveTraceContextCustomizer;
import com.getboot.observability.spi.TraceContextCustomizer;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * SkyWalking Trace 上下文定制器。
 *
 * <p>用于将 SkyWalking 当前 TraceId 注入到日志 MDC 中，便于日志与链路追踪系统关联。</p>
 *
 * @author qiheng
 */
public class SkywalkingTraceContextCustomizer implements TraceContextCustomizer, ReactiveTraceContextCustomizer {

    /**
     * SkyWalking 配置。
     */
    private final ObservabilitySkywalkingProperties properties;

    /**
     * 创建 SkyWalking 上下文定制器。
     *
     * @param properties SkyWalking 配置
     */
    public SkywalkingTraceContextCustomizer(ObservabilitySkywalkingProperties properties) {
        this.properties = properties;
    }

    /**
     * 为 Servlet 链路补充 SkyWalking MDC 字段。
     *
     * @param traceContext Trace 上下文
     * @return 待写入的 MDC 条目
     */
    @Override
    public Map<String, String> customize(TraceContext traceContext) {
        return resolveEntries();
    }

    /**
     * 为 WebFlux 链路补充 SkyWalking MDC 字段。
     *
     * @param traceContext 响应式 Trace 上下文
     * @return 待写入的 MDC 条目
     */
    @Override
    public Map<String, String> customize(ReactiveTraceContext traceContext) {
        return resolveEntries();
    }

    /**
     * 解析当前 SkyWalking TraceId 对应的 MDC 条目。
     *
     * @return 待写入的 MDC 条目
     */
    private Map<String, String> resolveEntries() {
        String skywalkingTraceId = SkywalkingTraceSupport.resolveTraceId();
        if (!StringUtils.hasText(skywalkingTraceId)) {
            return Map.of();
        }
        return Map.of(properties.getMdcKey(), skywalkingTraceId);
    }
}
