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
package com.getboot.observability.infrastructure.autoconfigure;

import com.getboot.observability.support.TraceTaskDecorator;
import com.getboot.observability.support.TraceTaskDecoratorBeanPostProcessor;
import com.getboot.observability.spi.TraceIdGenerator;
import com.getboot.support.api.trace.TraceContextHolder;
import com.getboot.support.infrastructure.autoconfigure.TraceContextPropagationAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    TraceContextPropagationAutoConfiguration.class,
                    ObservabilityAutoConfiguration.class
            ));

    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
        MDC.clear();
    }

    @Test
    void shouldRegisterAsyncTraceBeansByDefault() {
        contextRunner.run(context -> {
            assertTrue(context.containsBean("traceIdGenerator"));
            assertTrue(context.containsBean("getbootTraceTaskDecorator"));
            assertTrue(context.containsBean("traceTaskDecoratorBeanPostProcessor"));
            assertTrue(context.getBean(TraceIdGenerator.class) != null);
            assertTrue(context.getBean("getbootTraceTaskDecorator", TaskDecorator.class) instanceof TraceTaskDecorator);
            assertTrue(context.getBean(TraceTaskDecoratorBeanPostProcessor.class) != null);
        });
    }

    @Test
    void shouldPropagateTraceContextThroughTaskDecorator() {
        contextRunner.run(context -> {
            TaskDecorator taskDecorator = context.getBean("getbootTraceTaskDecorator", TaskDecorator.class);
            AtomicReference<String> traceId = new AtomicReference<>();
            AtomicReference<String> mdcTraceId = new AtomicReference<>();

            TraceContextHolder.bindTraceId("trace-async-001");
            MDC.put("traceId", "trace-async-001");

            Runnable decorated = taskDecorator.decorate(() -> {
                traceId.set(TraceContextHolder.getTraceId());
                mdcTraceId.set(MDC.get("traceId"));
            });

            TraceContextHolder.clear();
            MDC.clear();

            decorated.run();

            assertEquals("trace-async-001", traceId.get());
            assertEquals("trace-async-001", mdcTraceId.get());
            assertNull(TraceContextHolder.getTraceId());
            assertNull(MDC.get("traceId"));
        });
    }

    @Test
    void shouldSkipAsyncPropagationBeansWhenDisabled() {
        contextRunner
                .withPropertyValues("getboot.observability.trace.async-propagation-enabled=false")
                .run(context -> {
                    assertTrue(context.containsBean("traceIdGenerator"));
                    assertFalse(context.containsBean("getbootTraceTaskDecorator"));
                    assertFalse(context.containsBean("traceTaskDecoratorBeanPostProcessor"));
                });
    }

    @Test
    void shouldUseGetbootTaskDecoratorWhenCustomTaskDecoratorBeanExists() {
        contextRunner
                .withUserConfiguration(CustomTaskDecoratorConfiguration.class, AsyncExecutorConfiguration.class)
                .run(context -> {
                    TaskDecorator getbootTaskDecorator = context.getBean("getbootTraceTaskDecorator", TaskDecorator.class);
                    TaskDecorator customTaskDecorator = context.getBean("customTaskDecorator", TaskDecorator.class);
                    ThreadPoolTaskExecutor taskExecutor = context.getBean(ThreadPoolTaskExecutor.class);

                    Object appliedDecorator = ReflectionTestUtils.getField(taskExecutor, "taskDecorator");

                    assertSame(getbootTaskDecorator, appliedDecorator);
                    assertNotSame(customTaskDecorator, appliedDecorator);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomTaskDecoratorConfiguration {

        @Bean
        TaskDecorator customTaskDecorator() {
            return runnable -> runnable;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AsyncExecutorConfiguration {

        @Bean
        ThreadPoolTaskExecutor applicationTaskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(1);
            executor.setThreadNamePrefix("observability-test-");
            return executor;
        }
    }
}
