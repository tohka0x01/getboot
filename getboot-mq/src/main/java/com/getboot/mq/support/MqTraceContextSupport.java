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
package com.getboot.mq.support;

import com.getboot.mq.api.message.MqMessage;
import com.getboot.mq.api.properties.MqTraceProperties;
import com.getboot.support.api.trace.TraceContextHolder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * MQ Trace 上下文支撑工具。
 *
 * @author qiheng
 */
public class MqTraceContextSupport {

    /**
     * 默认写入 MDC 的 Trace 键名。
     */
    private static final String DEFAULT_TRACE_MDC_KEY = "traceId";

    /**
     * MQ Trace 配置。
     */
    private final MqTraceProperties traceProperties;

    /**
     * 创建 MQ Trace 上下文支撑工具。
     *
     * @param traceProperties MQ Trace 配置
     */
    public MqTraceContextSupport(MqTraceProperties traceProperties) {
        this.traceProperties = traceProperties;
    }

    /**
     * 判断当前是否启用 MQ Trace 透传。
     *
     * @return 启用 Trace 透传时返回 {@code true}
     */
    public boolean isEnabled() {
        return traceProperties != null && traceProperties.isEnabled();
    }

    /**
     * 返回当前使用的 Trace 消息头名称。
     *
     * @return Trace 消息头名称
     */
    public String getTraceHeaderName() {
        if (traceProperties != null && StringUtils.hasText(traceProperties.getHeaderName())) {
            return traceProperties.getHeaderName().trim();
        }
        return "TRACE_ID";
    }

    /**
     * 返回当前使用的 Trace MDC 键名。
     *
     * @return Trace MDC 键名
     */
    public String getTraceMdcKey() {
        if (traceProperties != null && StringUtils.hasText(traceProperties.getMdcKey())) {
            return traceProperties.getMdcKey().trim();
        }
        return DEFAULT_TRACE_MDC_KEY;
    }

    /**
     * 解析出站消息应当携带的 TraceId。
     *
     * @param message MQ 消息
     * @return 出站 TraceId
     */
    public String resolveOutboundTraceId(MqMessage message) {
        String currentTraceId = TraceContextHolder.getTraceId();
        if (StringUtils.hasText(currentTraceId)) {
            return currentTraceId.trim();
        }
        if (message != null && StringUtils.hasText(message.getTraceId())) {
            return message.getTraceId().trim();
        }
        return null;
    }

    /**
     * 从入站消息对象中提取 TraceId。
     *
     * @param source 入站消息来源对象
     * @return 提取到的 TraceId
     */
    public String resolveInboundTraceId(Object source) {
        if (!isEnabled() || source == null) {
            return null;
        }
        if (source instanceof Message<?> springMessage) {
            String traceId = normalizeTraceValue(springMessage.getHeaders().get(getTraceHeaderName()));
            if (StringUtils.hasText(traceId)) {
                return traceId;
            }
            return resolveInboundTraceId(springMessage.getPayload());
        }
        if (source instanceof MqMessage mqMessage && StringUtils.hasText(mqMessage.getTraceId())) {
            return mqMessage.getTraceId().trim();
        }
        if (source instanceof MessageExt messageExt) {
            String traceId = messageExt.getProperty(getTraceHeaderName());
            if (StringUtils.hasText(traceId)) {
                return traceId.trim();
            }
            return null;
        }
        if (source instanceof ConsumerRecord<?, ?> consumerRecord) {
            String traceId = resolveInboundTraceId(consumerRecord.headers());
            if (StringUtils.hasText(traceId)) {
                return traceId;
            }
            return resolveInboundTraceId(consumerRecord.value());
        }
        if (source instanceof Headers headers) {
            Header traceHeader = headers.lastHeader(getTraceHeaderName());
            if (traceHeader == null || traceHeader.value() == null) {
                return null;
            }
            return normalizeTraceValue(traceHeader.value());
        }
        if (source instanceof Header header) {
            return normalizeTraceValue(header.value());
        }
        if (source instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String traceId = resolveInboundTraceId(item);
                if (StringUtils.hasText(traceId)) {
                    return traceId;
                }
            }
        }
        return null;
    }

    /**
     * 打开一个新的 Trace 作用域，并在关闭时恢复原始上下文。
     *
     * @param traceId 待绑定的 TraceId
     * @return Trace 作用域
     */
    public TraceScope openScope(String traceId) {
        if (!StringUtils.hasText(traceId)) {
            return TraceScope.noop();
        }
        String normalizedTraceId = traceId.trim();
        String previousTraceId = TraceContextHolder.bindTraceId(normalizedTraceId);
        String traceMdcKey = getTraceMdcKey();
        String previousMdcTraceId = MDC.get(traceMdcKey);
        MDC.put(traceMdcKey, normalizedTraceId);
        return new TraceScope(previousTraceId, traceMdcKey, previousMdcTraceId, true);
    }

    /**
     * 将不同来源的 Trace 值规范化为字符串。
     *
     * @param traceValue 原始 Trace 值
     * @return 规范化后的 TraceId
     */
    private String normalizeTraceValue(Object traceValue) {
        if (traceValue instanceof byte[] bytes) {
            String traceId = new String(bytes, StandardCharsets.UTF_8);
            return StringUtils.hasText(traceId) ? traceId.trim() : null;
        }
        if (traceValue instanceof String traceId && StringUtils.hasText(traceId)) {
            return traceId.trim();
        }
        if (traceValue != null) {
            String traceId = traceValue.toString();
            return StringUtils.hasText(traceId) ? traceId.trim() : null;
        }
        return null;
    }

    /**
     * MQ Trace 作用域。
     *
     * @param previousTraceId 进入作用域前的 TraceId
     * @param traceMdcKey Trace 的 MDC 键名
     * @param previousMdcTraceId 进入作用域前 MDC 中的 Trace 值
     * @param active 当前作用域是否生效
     */
    public record TraceScope(
            String previousTraceId,
            String traceMdcKey,
            String previousMdcTraceId,
            boolean active) implements AutoCloseable {

        /**
         * 创建不执行任何恢复逻辑的空作用域。
         *
         * @return 空作用域
         */
        public static TraceScope noop() {
            return new TraceScope(null, null, null, false);
        }

        /**
         * 关闭作用域并恢复进入前的 Trace 上下文。
         */
        @Override
        public void close() {
            if (!active) {
                return;
            }
            TraceContextHolder.restoreTraceId(previousTraceId);
            if (previousMdcTraceId == null) {
                MDC.remove(traceMdcKey);
                return;
            }
            MDC.put(traceMdcKey, previousMdcTraceId);
        }
    }
}
