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
package com.getboot.mq.infrastructure.mqtt.producer;

import com.alibaba.fastjson2.JSON;
import com.getboot.mq.api.message.MqMessage;
import com.getboot.mq.api.model.MqSendReceipt;
import com.getboot.mq.api.model.MqTransactionReceipt;
import com.getboot.mq.api.producer.MqMessageProducer;
import com.getboot.mq.api.properties.MqProperties;
import com.getboot.mq.api.properties.MqTraceProperties;
import com.getboot.mq.infrastructure.mqtt.support.NettyMqttPublishingGateway;
import com.getboot.mq.spi.MqMessageHeadersCustomizer;
import com.getboot.mq.support.MqDestination;
import com.getboot.mq.support.MqTraceContextSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MQTT 消息生产者实现。
 *
 * <p>首版只承接统一发送门面，不把事务、延迟和消费端语义误抽成通用能力。</p>
 *
 * @author qiheng
 */
public class MqttMqMessageProducer implements MqMessageProducer {

    /**
     * MQTT 中用于保留能力层标签语义的消息头名称。
     */
    public static final String TAG_HEADER = "GETBOOT_MQ_TAG";

    /**
     * MQTT 主题重写头名称。
     */
    public static final String TOPIC_HEADER = "GETBOOT_MQ_MQTT_TOPIC";

    /**
     * MQTT QoS 重写头名称。
     */
    public static final String QOS_HEADER = "GETBOOT_MQ_MQTT_QOS";

    /**
     * MQTT Retained 重写头名称。
     */
    public static final String RETAINED_HEADER = "GETBOOT_MQ_MQTT_RETAINED";

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(MqttMqMessageProducer.class);

    /**
     * MQTT 发布网关。
     */
    private final NettyMqttPublishingGateway publishingGateway;

    /**
     * MQTT 能力配置。
     */
    private final MqProperties.Mqtt mqttProperties;

    /**
     * MQ Trace 上下文支撑工具。
     */
    private final MqTraceContextSupport traceContextSupport;

    /**
     * 消息头定制器集合。
     */
    private final List<MqMessageHeadersCustomizer> messageHeadersCustomizers;

    /**
     * 创建 MQTT 消息生产者。
     *
     * @param publishingGateway MQTT 发布网关
     * @param mqttProperties MQTT 能力配置
     * @param traceProperties MQ Trace 配置
     * @param messageHeadersCustomizers 消息头定制器集合
     */
    public MqttMqMessageProducer(
            NettyMqttPublishingGateway publishingGateway,
            MqProperties.Mqtt mqttProperties,
            MqTraceProperties traceProperties,
            List<MqMessageHeadersCustomizer> messageHeadersCustomizers) {
        this.publishingGateway = publishingGateway;
        this.mqttProperties = mqttProperties == null ? new MqProperties.Mqtt() : mqttProperties;
        this.traceContextSupport = new MqTraceContextSupport(traceProperties);
        this.messageHeadersCustomizers = messageHeadersCustomizers == null ? List.of() : List.copyOf(messageHeadersCustomizers);
    }

    /**
     * 按主题和标签发送 MQTT 消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @return 发送回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt send(String topic, String tag, T message) {
        return send(MqDestination.of(topic, tag).destination(), message);
    }

    /**
     * 按逻辑目标地址发送 MQTT 消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @return 发送回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt send(String destination, T message) {
        MqDestination mqDestination = MqDestination.parse(destination);
        Map<String, Object> headers = buildHeaders(mqDestination, message);
        try {
            publishingGateway.publish(
                    resolveTopic(headers, mqDestination),
                    JSON.toJSONString(message).getBytes(StandardCharsets.UTF_8),
                    resolveQos(headers),
                    resolveRetained(headers)
            );
            log.info("[{}] MQTT message sent successfully. messageId={}", destination, message.getMessageId());
            return new MqSendReceipt(destination, message.getMessageId());
        } catch (Exception ex) {
            log.error("[{}] Failed to send MQTT message.", destination, ex);
            throw new IllegalStateException("Failed to send MQTT message.", ex);
        }
    }

    /**
     * 按主题和标签异步发送 MQTT 消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @return 异步发送结果
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> CompletableFuture<MqSendReceipt> asyncSend(String topic, String tag, T message) {
        return asyncSend(MqDestination.of(topic, tag).destination(), message);
    }

    /**
     * 按逻辑目标地址异步发送 MQTT 消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @return 异步发送结果
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> CompletableFuture<MqSendReceipt> asyncSend(String destination, T message) {
        if (!mqttProperties.isAsync()) {
            return CompletableFuture.completedFuture(send(destination, message));
        }
        return CompletableFuture.supplyAsync(() -> send(destination, message));
    }

    /**
     * MQTT 不支持 RocketMQ 风格的延迟级别发送。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @param delayLevel 延迟级别
     * @return 不会返回
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt sendWithDelay(String topic, String tag, T message, int delayLevel) {
        throw unsupported("MQTT producer does not support RocketMQ delayLevel based delayed send.");
    }

    /**
     * MQTT 不支持 RocketMQ 风格的延迟级别发送。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @param delayLevel 延迟级别
     * @return 不会返回
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt sendWithDelay(String destination, T message, int delayLevel) {
        throw unsupported("MQTT producer does not support RocketMQ delayLevel based delayed send.");
    }

    /**
     * 逐条发送 MQTT 批量消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param messages 消息列表
     * @return 最后一条消息的发送回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt sendBatch(String topic, String tag, List<T> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages must not be empty.");
        }
        String destination = MqDestination.of(topic, tag).destination();
        MqSendReceipt lastReceipt = null;
        for (T message : messages) {
            lastReceipt = send(destination, message);
        }
        return lastReceipt;
    }

    /**
     * MQTT 不承诺按 hashKey 路由的顺序语义。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @param hashKey 顺序键
     * @return 不会返回
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt sendOrderly(String topic, String tag, T message, String hashKey) {
        throw unsupported("MQTT producer does not support keyed orderly dispatch semantics.");
    }

    /**
     * MQTT 不承诺按 hashKey 路由的顺序语义。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @param hashKey 顺序键
     * @return 不会返回
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt sendOrderly(String destination, T message, String hashKey) {
        throw unsupported("MQTT producer does not support keyed orderly dispatch semantics.");
    }

    /**
     * MQTT 不支持 RocketMQ 风格的事务消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @param arg 事务参数
     * @return 不会返回
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqTransactionReceipt sendTransaction(String destination, T message, Object arg) {
        throw unsupported("MQTT producer does not support transactional message semantics.");
    }

    /**
     * MQTT 不支持 RocketMQ 风格的事务消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @param arg 事务参数
     * @return 不会返回
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqTransactionReceipt sendTransaction(String topic, String tag, T message, Object arg) {
        throw unsupported("MQTT producer does not support transactional message semantics.");
    }

    /**
     * 构建 MQTT 发送头。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @return 头信息
     * @param <T> 消息类型
     */
    private <T extends MqMessage> Map<String, Object> buildHeaders(MqDestination destination, T message) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(TOPIC_HEADER, destination.topic());
        headers.put(QOS_HEADER, mqttProperties.getDefaultQos());
        headers.put(RETAINED_HEADER, mqttProperties.isRetained());
        if (StringUtils.hasText(destination.tag())) {
            headers.put(TAG_HEADER, destination.tag());
        }
        if (traceContextSupport.isEnabled()) {
            headers.put(traceContextSupport.getTraceHeaderName(), generateTraceId(message));
        }
        messageHeadersCustomizers.forEach(customizer -> customizer.customize(headers, message, destination.destination()));
        return headers;
    }

    /**
     * 解析最终发送主题。
     *
     * @param headers 头信息
     * @param destination 逻辑目标地址
     * @return 发送主题
     */
    private String resolveTopic(Map<String, Object> headers, MqDestination destination) {
        Object topic = headers.get(TOPIC_HEADER);
        if (topic instanceof String text && StringUtils.hasText(text)) {
            return text.trim();
        }
        return destination.topic();
    }

    /**
     * 解析最终 QoS。
     *
     * @param headers 头信息
     * @return QoS 级别
     */
    private int resolveQos(Map<String, Object> headers) {
        Object qos = headers.get(QOS_HEADER);
        if (qos instanceof Number number) {
            return number.intValue();
        }
        if (qos instanceof String text && StringUtils.hasText(text)) {
            return Integer.parseInt(text.trim());
        }
        return mqttProperties.getDefaultQos();
    }

    /**
     * 解析最终 retained 标识。
     *
     * @param headers 头信息
     * @return retained 标识
     */
    private boolean resolveRetained(Map<String, Object> headers) {
        Object retained = headers.get(RETAINED_HEADER);
        if (retained instanceof Boolean flag) {
            return flag;
        }
        if (retained instanceof String text && StringUtils.hasText(text)) {
            return Boolean.parseBoolean(text.trim());
        }
        return mqttProperties.isRetained();
    }

    /**
     * 生成并回填消息 TraceId。
     *
     * @param message 消息体
     * @return TraceId
     */
    private String generateTraceId(MqMessage message) {
        String traceId = traceContextSupport.resolveOutboundTraceId(message);
        if (!StringUtils.hasText(traceId)) {
            traceId = message.getTraceId();
        }
        message.setTraceId(traceId);
        return traceId;
    }

    /**
     * 创建不支持异常。
     *
     * @param message 异常消息
     * @return 不支持异常
     */
    private UnsupportedOperationException unsupported(String message) {
        return new UnsupportedOperationException(message);
    }
}
