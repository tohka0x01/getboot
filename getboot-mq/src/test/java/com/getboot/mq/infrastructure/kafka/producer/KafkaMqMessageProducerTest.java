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
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        @SuppressWarnings("unchecked")
        KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(any(Message.class))).thenReturn(completedSendFuture());

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

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(kafkaTemplate).send(messageCaptor.capture());
        Message<?> outboundMessage = messageCaptor.getValue();

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
        @SuppressWarnings("unchecked")
        KafkaTemplate<Object, Object> kafkaTemplate = mock(KafkaTemplate.class);
        KafkaMqMessageProducer producer = new KafkaMqMessageProducer(kafkaTemplate);
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
    private CompletableFuture<SendResult<Object, Object>> completedSendFuture() {
        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>("orders", "key", "value");
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("orders", 0), 0, 0, 0, 0, 0);
        return CompletableFuture.completedFuture(new SendResult<>(producerRecord, metadata));
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
