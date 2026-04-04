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
package com.getboot.mq.infrastructure.mqtt.autoconfigure;

import com.getboot.mq.api.producer.MqMessageProducer;
import com.getboot.mq.infrastructure.mqtt.producer.MqttMqMessageProducer;
import com.getboot.mq.infrastructure.mqtt.support.NettyMqttPublishingGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQTT 自动配置测试。
 *
 * @author qiheng
 */
class MqttMqEnhancementAutoConfigurationTest {

    /**
     * 上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MqttMqEnhancementAutoConfiguration.class))
            .withPropertyValues(
                    "getboot.mq.enabled=true",
                    "getboot.mq.type=mqtt",
                    "getboot.mq.mqtt.enabled=true",
                    "getboot.mq.mqtt.server-uri=tcp://127.0.0.1:1883",
                    "getboot.mq.mqtt.client-id=demo-mqtt-client"
            );

    /**
     * 验证 MQTT 自动配置会注册默认客户端工厂和消息生产者。
     */
    @Test
    void shouldRegisterDefaultMqttBeans() {
        contextRunner.run(context -> {
            assertTrue(context.containsBean("nettyMqttPublishingGateway"));
            assertInstanceOf(NettyMqttPublishingGateway.class, context.getBean(NettyMqttPublishingGateway.class));
            assertInstanceOf(MqttMqMessageProducer.class, context.getBean(MqMessageProducer.class));
        });
    }

    /**
     * 验证切换为非 MQTT 类型时不会注册 MQTT 相关 Bean。
     */
    @Test
    void shouldSkipMqttBeansWhenTypeIsNotMqtt() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MqttMqEnhancementAutoConfiguration.class))
                .withPropertyValues(
                        "getboot.mq.enabled=true",
                        "getboot.mq.type=kafka",
                        "getboot.mq.mqtt.enabled=true"
                )
                .run(context -> {
                    assertFalse(context.containsBean("nettyMqttPublishingGateway"));
                    assertFalse(context.containsBean("mqMessageProducer"));
                });
    }
}
