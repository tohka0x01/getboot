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

/**
 * {@link ObservabilityAutoConfiguration} 测试。
 *
 * @author qiheng
 */
class ObservabilityAutoConfigurationTest {

    /**
     * 应用上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    TraceContextPropagationAutoConfiguration.class,
                    ObservabilityAutoConfiguration.class
            ));

    /**
     * 清理测试过程中写入的 Trace 上下文。
     */
    @AfterEach
    void tearDown() {
        TraceContextHolder.clear();
        MDC.clear();
    }

    /**
     * 验证默认情况下会注册异步 Trace 相关 Bean。
     */
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

    /**
     * 验证任务装饰器能够透传 Trace 上下文与 MDC。
     */
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

    /**
     * 验证关闭异步传播后不会注册相关 Bean。
     */
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

    /**
     * 验证短 TraceId 生成器只生成小写字母和数字。
     */
    @Test
    void shouldRegisterShortTraceIdGeneratorWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "getboot.observability.trace.id-generator=short",
                        "getboot.observability.trace.short-length=12"
                )
                .run(context -> {
                    String traceId = context.getBean(TraceIdGenerator.class).generate();

                    assertEquals(12, traceId.length());
                    assertTrue(traceId.matches("[0-9a-z]+"));
                });
    }

    /**
     * 验证存在自定义任务装饰器时仍会优先挂载 GetBoot 的 Trace 装饰器。
     */
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

    /**
     * 自定义任务装饰器配置。
     */
    @Configuration(proxyBeanMethods = false)
    static class CustomTaskDecoratorConfiguration {

        /**
         * 注册业务自定义任务装饰器。
         *
         * @return 自定义任务装饰器
         */
        @Bean
        TaskDecorator customTaskDecorator() {
            return runnable -> runnable;
        }
    }

    /**
     * 异步执行器配置。
     */
    @Configuration(proxyBeanMethods = false)
    static class AsyncExecutorConfiguration {

        /**
         * 注册测试用线程池执行器。
         *
         * @return 线程池执行器
         */
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
