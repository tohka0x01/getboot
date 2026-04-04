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
import com.alibaba.fastjson2.JSONObject;
import com.getboot.mq.api.message.MqMessage;
import com.getboot.mq.api.model.MqSendReceipt;
import com.getboot.mq.api.properties.MqProperties;
import com.getboot.mq.api.properties.MqTraceProperties;
import com.getboot.support.api.trace.TraceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link MqttMqMessageProducer} 测试。
 *
 * @author qiheng
 */
class MqttMqMessageProducerTest {

    /**
     * 清理线程级 Trace 上下文。
     */
    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
    }

    /**
     * 验证 MQTT 生产者会将主题、QoS、retained 和消息体交给消息处理器。
     */
    @Test
    void shouldSendMqttMessageWithTraceAndCustomHeaders() {
        CapturingMessageHandler messageHandler = new CapturingMessageHandler();
        MqProperties.Mqtt mqttProperties = new MqProperties.Mqtt();
        mqttProperties.setDefaultQos(1);
        mqttProperties.setRetained(false);

        MqttMqMessageProducer producer = new MqttMqMessageProducer(
                messageHandler,
                mqttProperties,
                new MqTraceProperties(),
                List.of((headers, message, destination) -> {
                    headers.put(MqttMqMessageProducer.TOPIC_HEADER, "devices/status");
                    headers.put(MqttMqMessageProducer.QOS_HEADER, 0);
                    headers.put(MqttMqMessageProducer.RETAINED_HEADER, true);
                })
        );

        DemoMessage message = new DemoMessage();
        message.setBizKey("device-1");
        TraceContextHolder.bindTraceId("trace-mqtt-1");

        MqSendReceipt receipt = producer.send("devices", "status", message);
        JSONObject payload = JSON.parseObject((String) messageHandler.message.getPayload());

        assertEquals("devices/status", messageHandler.message.getHeaders().get(MqttHeaders.TOPIC));
        assertEquals(0, messageHandler.message.getHeaders().get(MqttHeaders.QOS));
        assertEquals(true, messageHandler.message.getHeaders().get(MqttHeaders.RETAINED));
        assertEquals("trace-mqtt-1", messageHandler.message.getHeaders().get("TRACE_ID"));
        assertEquals("device-1", payload.getString("bizKey"));
        assertEquals("trace-mqtt-1", payload.getString("traceId"));
        assertEquals("devices:status", receipt.destination());
        assertEquals(message.getMessageId(), receipt.messageId());
    }

    /**
     * 验证 MQTT 实现会拒绝延迟、顺序和事务消息操作。
     */
    @Test
    void shouldRejectUnsupportedDelayOrderlyAndTransactionOperations() {
        MqttMqMessageProducer producer = new MqttMqMessageProducer(
                new CapturingMessageHandler(),
                new MqProperties.Mqtt(),
                new MqTraceProperties(),
                List.of()
        );
        DemoMessage message = new DemoMessage();

        assertThrows(UnsupportedOperationException.class,
                () -> producer.sendWithDelay("devices", "status", message, 3));
        assertThrows(UnsupportedOperationException.class,
                () -> producer.sendOrderly("devices:status", message, "hash-key"));
        assertThrows(UnsupportedOperationException.class,
                () -> producer.sendTransaction("devices:status", message, new Object()));
    }

    /**
     * 验证同步模式下异步发送会立即完成。
     */
    @Test
    void shouldReturnCompletedFutureWhenAsyncDisabled() {
        MqProperties.Mqtt mqttProperties = new MqProperties.Mqtt();
        mqttProperties.setAsync(false);
        MqttMqMessageProducer producer = new MqttMqMessageProducer(
                new CapturingMessageHandler(),
                mqttProperties,
                new MqTraceProperties(),
                List.of()
        );

        assertFalse(producer.asyncSend("devices", "status", new DemoMessage()).isCompletedExceptionally());
    }

    /**
     * 测试用消息处理器。
     */
    private static final class CapturingMessageHandler implements MessageHandler {

        /**
         * 最近一次发送消息。
         */
        private Message<?> message;

        /**
         * 捕获最近一次发送内容。
         *
         * @param message Spring 消息
         */
        @Override
        public void handleMessage(Message<?> message) {
            this.message = message;
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
