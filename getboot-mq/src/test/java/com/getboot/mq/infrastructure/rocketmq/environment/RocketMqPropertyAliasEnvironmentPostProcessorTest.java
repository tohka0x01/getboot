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
package com.getboot.mq.infrastructure.rocketmq.environment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link RocketMqPropertyAliasEnvironmentPostProcessor} 测试。
 *
 * @author qiheng
 */
class RocketMqPropertyAliasEnvironmentPostProcessorTest {

    /**
     * 验证 GetBoot RocketMQ 前缀会桥接到原生 RocketMQ 前缀。
     */
    @Test
    void shouldAliasGetbootRocketMqPropertiesToNativePrefix() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("source", Map.of(
                "getboot.mq.rocketmq.name-server", "127.0.0.1:9876",
                "getboot.mq.rocketmq.producer.group", "demo-producer",
                "getboot.mq.rocketmq.producer.send-message-timeout", "3000"
        )));

        RocketMqPropertyAliasEnvironmentPostProcessor processor = new RocketMqPropertyAliasEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertEquals("127.0.0.1:9876", environment.getProperty("rocketmq.name-server"));
        assertEquals("demo-producer", environment.getProperty("rocketmq.producer.group"));
        assertEquals("3000", environment.getProperty("rocketmq.producer.send-message-timeout"));
    }

    /**
     * 验证显式声明的 RocketMQ 原生配置不会被桥接覆盖。
     */
    @Test
    void shouldNotOverrideExistingNativeRocketMqProperties() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("source", Map.of(
                "getboot.mq.rocketmq.name-server", "127.0.0.1:9876",
                "rocketmq.name-server", "127.0.0.1:19876"
        )));

        RocketMqPropertyAliasEnvironmentPostProcessor processor = new RocketMqPropertyAliasEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertEquals("127.0.0.1:19876", environment.getProperty("rocketmq.name-server"));
    }
}
