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
package com.getboot.mq.infrastructure.kafka.producer;

import com.getboot.mq.api.message.MqMessage;
import com.getboot.mq.api.model.MqSendReceipt;
import com.getboot.mq.api.properties.MqTraceProperties;
import com.getboot.support.api.trace.TraceContextHolder;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link KafkaMqMessageProducer} 测试。
 *
 * @author qiheng
 */
class KafkaMqMessageProducerTest {

    /**
     * 清理线程级 Trace 上下文。
     */
    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
    }

    /**
     * 验证 Kafka 生产者会按逻辑目标地址写入主题、标签、Trace 和自定义消息头。
     */
    @Test
    void shouldSendKafkaMessageWithLogicalDestinationAndTraceHeader() {
        TestKafkaTemplate kafkaTemplate = new TestKafkaTemplate();

        MqTraceProperties traceProperties = new MqTraceProperties();
        KafkaMqMessageProducer producer = new KafkaMqMessageProducer(
                kafkaTemplate,
                traceProperties,
                List.of((headers, message, destination) -> headers.put("tenant", "demo"))
        );

        DemoMessage message = new DemoMessage();
        message.setBizKey("order-1");
        TraceContextHolder.bindTraceId("trace-out-1");

        MqSendReceipt receipt = producer.send("orders", "created", message);

        Message<?> outboundMessage = kafkaTemplate.getLastMessage();

        assertEquals("orders", outboundMessage.getHeaders().get(KafkaHeaders.TOPIC));
        assertEquals("order-1", outboundMessage.getHeaders().get(KafkaHeaders.KEY));
        assertEquals("created", outboundMessage.getHeaders().get(KafkaMqMessageProducer.TAG_HEADER));
        assertEquals("trace-out-1", outboundMessage.getHeaders().get("TRACE_ID"));
        assertEquals("demo", outboundMessage.getHeaders().get("tenant"));
        assertEquals(message, outboundMessage.getPayload());
        assertEquals("orders:created", receipt.destination());
        assertEquals(message.getMessageId(), receipt.messageId());
    }

    /**
     * 验证 Kafka 实现会拒绝 RocketMQ 专属的延迟和事务消息操作。
     */
    @Test
    void shouldRejectRocketMqSpecificDelayAndTransactionOperations() {
        KafkaMqMessageProducer producer = new KafkaMqMessageProducer(new TestKafkaTemplate());
        DemoMessage message = new DemoMessage();
        message.setBizKey("order-2");

        assertThrows(UnsupportedOperationException.class,
                () -> producer.sendWithDelay("orders", "created", message, 3));
        assertThrows(UnsupportedOperationException.class,
                () -> producer.sendTransaction("orders:created", message, new Object()));
    }

    /**
     * 创建一个已完成的 Kafka 发送结果。
     *
     * @return 已完成的 Kafka 发送结果
     */
    private static CompletableFuture<SendResult<Object, Object>> completedSendFuture() {
        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>("orders", "key", "value");
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("orders", 0), 0, 0, 0, 0, 0);
        return CompletableFuture.completedFuture(new SendResult<>(producerRecord, metadata));
    }

    /**
     * 测试用 KafkaTemplate，避免依赖 Mockito。
     */
    private static final class TestKafkaTemplate extends KafkaTemplate<Object, Object> {

        /**
         * 最近一次发送的消息。
         */
        private Message<?> lastMessage;

        /**
         * 创建测试用 KafkaTemplate。
         */
        private TestKafkaTemplate() {
            super(new TestProducerFactory());
        }

        /**
         * 捕获发送消息并返回成功结果。
         *
         * @param message 待发送消息
         * @return 已完成发送结果
         */
        @Override
        public CompletableFuture<SendResult<Object, Object>> send(Message<?> message) {
            this.lastMessage = message;
            return completedSendFuture();
        }

        /**
         * 返回最近一次发送的消息。
         *
         * @return 最近一次消息
         */
        private Message<?> getLastMessage() {
            return lastMessage;
        }
    }

    /**
     * 测试用 ProducerFactory，占位即可。
     */
    private static final class TestProducerFactory implements ProducerFactory<Object, Object> {

        /**
         * 当前测试不会真正创建 Kafka Producer。
         *
         * @return 不返回
         */
        @Override
        public Producer<Object, Object> createProducer() {
            throw new UnsupportedOperationException("Producer creation is not required in tests.");
        }
    }

    /**
     * 测试用消息类型。
     */
    static class DemoMessage extends MqMessage {

        /**
         * 返回测试消息类型标识。
         *
         * @return 消息类型标识
         */
        @Override
        public String getMessageType() {
            return "demo";
        }
    }
}
