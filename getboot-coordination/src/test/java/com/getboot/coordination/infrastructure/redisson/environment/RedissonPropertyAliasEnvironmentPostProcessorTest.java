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
package com.getboot.coordination.infrastructure.redisson.environment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link RedissonPropertyAliasEnvironmentPostProcessor} 测试。
 *
 * @author qiheng
 */
class RedissonPropertyAliasEnvironmentPostProcessorTest {

    /**
     * 验证 GetBoot Redisson 配置前缀会桥接到 Redisson 原生前缀。
     */
    @Test
    void shouldAliasGetbootRedissonPropertiesToNativePrefix() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("source", Map.of(
                "getboot.coordination.redisson.file", "classpath:redisson/demo.yaml",
                "getboot.coordination.redisson.config", "{\"threads\":4}"
        )));

        RedissonPropertyAliasEnvironmentPostProcessor processor = new RedissonPropertyAliasEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertEquals("classpath:redisson/demo.yaml", environment.getProperty("spring.redis.redisson.file"));
        assertEquals("{\"threads\":4}", environment.getProperty("spring.redis.redisson.config"));
    }

    /**
     * 验证已显式声明的 Redisson 原生配置不会被别名覆盖。
     */
    @Test
    void shouldNotOverrideExistingNativeRedissonProperties() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("source", Map.of(
                "getboot.coordination.redisson.file", "classpath:redisson/demo.yaml",
                "spring.redis.redisson.file", "classpath:redisson/native.yaml"
        )));

        RedissonPropertyAliasEnvironmentPostProcessor processor = new RedissonPropertyAliasEnvironmentPostProcessor();
        processor.postProcessEnvironment(environment, new SpringApplication(Object.class));

        assertEquals("classpath:redisson/native.yaml", environment.getProperty("spring.redis.redisson.file"));
    }
}
