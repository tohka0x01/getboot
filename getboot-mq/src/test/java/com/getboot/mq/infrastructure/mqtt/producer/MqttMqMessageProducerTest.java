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
import com.getboot.mq.infrastructure.mqtt.support.NettyMqttPublishingGateway;
import com.getboot.support.api.trace.TraceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
     * 验证 MQTT 生产者会将主题、QoS、retained 和消息体交给发布网关。
     */
    @Test
    void shouldSendMqttMessageWithTraceAndCustomHeaders() {
        CapturingPublishingGateway publishingGateway = new CapturingPublishingGateway();
        MqProperties.Mqtt mqttProperties = new MqProperties.Mqtt();
        mqttProperties.setDefaultQos(1);
        mqttProperties.setRetained(false);

        MqttMqMessageProducer producer = new MqttMqMessageProducer(
                publishingGateway,
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
        JSONObject payload = JSON.parseObject(new String(publishingGateway.payloadBytes));

        assertEquals("devices/status", publishingGateway.topic);
        assertEquals(0, publishingGateway.qos);
        assertEquals(true, publishingGateway.retained);
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
                new CapturingPublishingGateway(),
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
                new CapturingPublishingGateway(),
                mqttProperties,
                new MqTraceProperties(),
                List.of()
        );

        assertFalse(producer.asyncSend("devices", "status", new DemoMessage()).isCompletedExceptionally());
    }

    /**
     * 测试用发布网关。
     */
    private static final class CapturingPublishingGateway extends NettyMqttPublishingGateway {

        /**
         * 最近一次发送主题。
         */
        private String topic;

        /**
         * 最近一次发送负载。
         */
        private byte[] payloadBytes;

        /**
         * 最近一次发送 QoS。
         */
        private int qos;

        /**
         * 最近一次发送 retained 标识。
         */
        private boolean retained;

        /**
         * 创建测试用发布网关。
         */
        private CapturingPublishingGateway() {
            super(new MqProperties.Mqtt());
        }

        /**
         * 捕获最近一次发送内容。
         *
         * @param topic 主题
         * @param payload 负载
         * @param qos QoS
         * @param retained retained 标识
         */
        @Override
        public void publish(String topic, byte[] payload, int qos, boolean retained) {
            this.topic = topic;
            this.payloadBytes = payload;
            this.qos = qos;
            this.retained = retained;
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
