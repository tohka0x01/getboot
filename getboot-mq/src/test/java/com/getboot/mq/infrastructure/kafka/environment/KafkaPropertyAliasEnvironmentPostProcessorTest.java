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
package com.getboot.mq.infrastructure.kafka.environment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link KafkaPropertyAliasEnvironmentPostProcessor} 测试。
 *
 * @author qiheng
 */
class KafkaPropertyAliasEnvironmentPostProcessorTest {

    /**
     * 验证 GetBoot Kafka 前缀会桥接到 Spring Kafka 原生前缀。
     */
    @Test
    void shouldAliasGetbootKafkaPropertiesToSpringKafkaPrefix() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("source", Map.of(
                "getboot.mq.kafka.bootstrap-servers", "127.0.0.1:9092",
                "getboot.mq.kafka.producer.key-serializer", "org.apache.kafka.common.serialization.StringSerializer",
                "getboot.mq.kafka.producer.value-serializer",
                "org.springframework.kafka.support.serializer.JsonSerializer"
        )));

        KafkaPropertyAliasEnvironmentPostProcessor processor = new KafkaPropertyAliasEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertEquals("127.0.0.1:9092", environment.getProperty("spring.kafka.bootstrap-servers"));
        assertEquals(
                "org.apache.kafka.common.serialization.StringSerializer",
                environment.getProperty("spring.kafka.producer.key-serializer")
        );
        assertEquals(
                "org.springframework.kafka.support.serializer.JsonSerializer",
                environment.getProperty("spring.kafka.producer.value-serializer")
        );
    }

    /**
     * 验证显式声明的 Spring Kafka 原生配置不会被桥接覆盖。
     */
    @Test
    void shouldNotOverrideExistingSpringKafkaProperties() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("source", Map.of(
                "getboot.mq.kafka.bootstrap-servers", "127.0.0.1:9092",
                "spring.kafka.bootstrap-servers", "127.0.0.1:19092"
        )));

        KafkaPropertyAliasEnvironmentPostProcessor processor = new KafkaPropertyAliasEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertEquals("127.0.0.1:19092", environment.getProperty("spring.kafka.bootstrap-servers"));
    }
}
