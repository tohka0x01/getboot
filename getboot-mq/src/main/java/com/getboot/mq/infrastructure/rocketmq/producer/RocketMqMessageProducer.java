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
package com.getboot.mq.infrastructure.rocketmq.producer;

import com.getboot.mq.api.message.MqMessage;
import com.getboot.mq.api.model.MqSendReceipt;
import com.getboot.mq.api.model.MqTransactionReceipt;
import com.getboot.mq.api.producer.MqMessageProducer;
import com.getboot.mq.api.properties.MqTraceProperties;
import com.getboot.mq.spi.MqMessageHeadersCustomizer;
import com.getboot.mq.support.MqDestination;
import com.getboot.mq.support.MqTraceContextSupport;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * RocketMQ 消息生产者实现。
 *
 * <p>负责封装 RocketMQ 同步、异步、延迟、顺序与事务消息发送能力。</p>
 *
 * @author qiheng
 */
public class RocketMqMessageProducer implements MqMessageProducer {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(RocketMqMessageProducer.class);

    /**
     * RocketMQ 模板。
     */
    private final RocketMQTemplate template;

    /**
     * MQ Trace 上下文支撑工具。
     */
    private final MqTraceContextSupport traceContextSupport;

    /**
     * 消息头定制器集合。
     */
    private final List<MqMessageHeadersCustomizer> messageHeadersCustomizers;

    /**
     * 创建使用默认 Trace 配置的 RocketMQ 消息生产者。
     *
     * @param template RocketMQ 模板
     */
    public RocketMqMessageProducer(RocketMQTemplate template) {
        this(template, new MqTraceProperties(), List.of());
    }

    /**
     * 创建 RocketMQ 消息生产者。
     *
     * @param template RocketMQ 模板
     * @param traceProperties MQ Trace 配置
     * @param messageHeadersCustomizers 消息头定制器集合
     */
    public RocketMqMessageProducer(RocketMQTemplate template,
                                   MqTraceProperties traceProperties,
                                   List<MqMessageHeadersCustomizer> messageHeadersCustomizers) {
        this.template = template;
        this.traceContextSupport = new MqTraceContextSupport(traceProperties);
        this.messageHeadersCustomizers = messageHeadersCustomizers == null ? List.of() : List.copyOf(messageHeadersCustomizers);
    }

    /**
     * 按主题和标签发送 RocketMQ 消息。
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
     * 按逻辑目标地址发送 RocketMQ 消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @return 发送回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt send(String destination, T message) {
        StopWatch watch = new StopWatch();
        try {
            watch.start();
            SendResult result = template.syncSend(destination, buildMessage(destination, message));
            log.info("[{}] Message sent successfully. messageId={}", destination, result.getMsgId());
            return toSendReceipt(destination, result);
        } catch (Exception e) {
            log.error("[{}] Failed to send message.", destination, e);
            throw new RocketMqSendException("Failed to send message.", e);
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Send cost={}ms", destination, watch.getTotalTimeMillis());
            }
        }
    }

    /**
     * 按主题和标签异步发送 RocketMQ 消息。
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
     * 按逻辑目标地址异步发送 RocketMQ 消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @return 异步发送结果
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> CompletableFuture<MqSendReceipt> asyncSend(String destination, T message) {
        CompletableFuture<MqSendReceipt> future = new CompletableFuture<>();
        template.asyncSend(destination, buildMessage(destination, message), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                future.complete(toSendReceipt(destination, sendResult));
                log.debug("[{}] Asynchronous send succeeded.", destination);
            }

            @Override
            public void onException(Throwable throwable) {
                future.completeExceptionally(throwable);
                log.error("[{}] Asynchronous send failed.", destination, throwable);
            }
        });
        return future;
    }

    /**
     * 按主题和标签发送延迟消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @param delayLevel 延迟级别
     * @return 发送回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt sendWithDelay(String topic, String tag, T message, int delayLevel) {
        validateDelayLevel(delayLevel);
        return sendWithDelay(MqDestination.of(topic, tag).destination(), message, delayLevel);
    }

    /**
     * 按逻辑目标地址发送延迟消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @param delayLevel 延迟级别
     * @return 发送回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt sendWithDelay(String destination, T message, int delayLevel) {
        try {
            SendResult result = template.syncSend(destination, buildMessage(destination, message), 3000, delayLevel);
            log.info("[{}] Delayed message sent successfully. delayLevel={}, messageId={}",
                    destination, delayLevel, result.getMsgId());
            return toSendReceipt(destination, result);
        } catch (Exception e) {
            log.error("[{}] Failed to send delayed message.", destination, e);
            throw new RocketMqSendException("Failed to send delayed message.", e);
        }
    }

    /**
     * 批量发送 RocketMQ 消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param messages 消息列表
     * @return 发送回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt sendBatch(String topic, String tag, List<T> messages) {
        String destination = MqDestination.of(topic, tag).destination();
        List<Message<T>> messageList = messages.stream()
                .map(message -> buildMessage(destination, message))
                .toList();
        try {
            SendResult result = template.syncSend(destination, messageList);
            log.info("[{}] Batch send succeeded. size={}", destination, messages.size());
            return toSendReceipt(destination, result);
        } catch (Exception e) {
            log.error("[{}] Batch send failed. size={}", destination, messages.size(), e);
            throw new RocketMqSendException("Failed to send batch messages.", e);
        }
    }

    /**
     * 按主题和标签发送顺序消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @param hashKey 顺序键
     * @return 发送回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt sendOrderly(String topic, String tag, T message, String hashKey) {
        return sendOrderly(MqDestination.of(topic, tag).destination(), message, hashKey);
    }

    /**
     * 按逻辑目标地址发送顺序消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @param hashKey 顺序键
     * @return 发送回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqSendReceipt sendOrderly(String destination, T message, String hashKey) {
        try {
            SendResult result = template.syncSendOrderly(destination, buildMessage(destination, message), hashKey);
            log.info("[{}] Ordered message sent successfully. messageId={}", destination, result.getMsgId());
            return toSendReceipt(destination, result);
        } catch (Exception e) {
            log.error("[{}] Failed to send ordered message.", destination, e);
            throw new RocketMqSendException("Failed to send ordered message.", e);
        }
    }

    /**
     * 按逻辑目标地址发送事务消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @param arg 事务参数
     * @return 事务消息回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqTransactionReceipt sendTransaction(String destination, T message, Object arg) {
        try {
            TransactionSendResult result = template.sendMessageInTransaction(destination, buildMessage(destination, message), arg);
            log.info("[{}] Transactional message submitted. transactionId={}", destination, result.getTransactionId());
            return new MqTransactionReceipt(destination, result.getMsgId(), result.getTransactionId());
        } catch (Exception e) {
            log.error("[{}] Failed to send transactional message.", destination, e);
            throw new RocketMqSendException("Failed to send transactional message.", e);
        }
    }

    /**
     * 按主题和标签发送事务消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @param arg 事务参数
     * @return 事务消息回执
     * @param <T> 消息类型
     */
    @Override
    public <T extends MqMessage> MqTransactionReceipt sendTransaction(String topic, String tag, T message, Object arg) {
        return sendTransaction(MqDestination.of(topic, tag).destination(), message, arg);
    }

    /**
     * 构建 RocketMQ 出站消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @return Spring 消息对象
     * @param <T> 消息类型
     */
    private <T extends MqMessage> Message<T> buildMessage(String destination, T message) {
        MqDestination mqDestination = MqDestination.parse(destination);
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(RocketMQHeaders.KEYS, message.getBizKey());
        if (traceContextSupport.isEnabled()) {
            headers.put(traceContextSupport.getTraceHeaderName(), generateTraceId(message));
        }
        if (StringUtils.hasText(mqDestination.topic())) {
            headers.put(RocketMQHeaders.TOPIC, mqDestination.topic());
        }
        messageHeadersCustomizers.forEach(customizer -> customizer.customize(headers, message, destination));
        MessageBuilder<T> builder = MessageBuilder.withPayload(message);
        headers.forEach(builder::setHeader);
        return builder.build();
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
            traceId = UUID.randomUUID().toString();
        }
        message.setTraceId(traceId);
        return traceId;
    }

    /**
     * 校验延迟级别是否合法。
     *
     * @param delayLevel 延迟级别
     */
    private void validateDelayLevel(int delayLevel) {
        if (delayLevel < 0 || delayLevel > 18) {
            throw new IllegalArgumentException("Invalid delay level: " + delayLevel);
        }
    }

    /**
     * 将发送结果转换为发送回执。
     *
     * @param destination 逻辑目标地址
     * @param sendResult 发送结果
     * @return 发送回执
     */
    private MqSendReceipt toSendReceipt(String destination, SendResult sendResult) {
        return new MqSendReceipt(destination, sendResult.getMsgId());
    }

    /**
     * RocketMQ 消息发送异常。
     */
    public static class RocketMqSendException extends RuntimeException {

        /**
         * 创建 RocketMQ 消息发送异常。
         *
         * @param message 异常消息
         * @param cause 根因异常
         */
        public RocketMqSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 返回 RocketMQ 模板。
     *
     * @return RocketMQ 模板
     */
    public RocketMQTemplate getTemplate() {
        return template;
    }

    /**
     * 返回 MQ Trace 上下文支撑工具。
     *
     * @return MQ Trace 上下文支撑工具
     */
    public MqTraceContextSupport getTraceContextSupport() {
        return traceContextSupport;
    }

    /**
     * 返回消息头定制器集合。
     *
     * @return 消息头定制器集合
     */
    public List<MqMessageHeadersCustomizer> getMessageHeadersCustomizers() {
        return messageHeadersCustomizers;
    }
}
