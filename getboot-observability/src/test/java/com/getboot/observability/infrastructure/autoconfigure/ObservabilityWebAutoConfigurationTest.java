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

import com.getboot.observability.infrastructure.reactor.autoconfigure.ReactorObservabilityAutoConfiguration;
import com.getboot.observability.infrastructure.reactor.support.ReactorContextPropagationInitializer;
import com.getboot.observability.infrastructure.servlet.autoconfigure.ServletObservabilityAutoConfiguration;
import com.getboot.observability.infrastructure.servlet.web.TraceMdcFilter;
import com.getboot.observability.infrastructure.webflux.autoconfigure.ReactiveObservabilityAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Servlet / WebFlux 可观测性自动配置测试。
 */
class ObservabilityWebAutoConfigurationTest {

    /**
     * Servlet 上下文运行器。
     */
    private final WebApplicationContextRunner servletRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObservabilityAutoConfiguration.class,
                    ServletObservabilityAutoConfiguration.class
            ))
            .withPropertyValues("getboot.observability.trace.async-propagation-enabled=false");

    /**
     * Reactive 上下文运行器。
     */
    private final ReactiveWebApplicationContextRunner reactiveRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObservabilityAutoConfiguration.class,
                    ReactiveObservabilityAutoConfiguration.class,
                    ReactorObservabilityAutoConfiguration.class
            ))
            .withPropertyValues("getboot.observability.trace.async-propagation-enabled=false");

    /**
     * 清理 Reactor 全局钩子，避免污染其他测试。
     */
    @AfterEach
    void tearDown() {
        Hooks.disableAutomaticContextPropagation();
    }

    /**
     * 验证 Servlet Web 应用会注册 Trace 过滤器。
     */
    @Test
    void shouldRegisterServletTraceFilterWhenTraceEnabled() {
        servletRunner.run(context -> {
            @SuppressWarnings("unchecked")
            FilterRegistrationBean<TraceMdcFilter> registrationBean =
                    context.getBean("traceMdcFilter", FilterRegistrationBean.class);
            assertInstanceOf(TraceMdcFilter.class, registrationBean.getFilter());
            assertEquals(Ordered.HIGHEST_PRECEDENCE, registrationBean.getOrder());
        });
    }

    /**
     * 验证关闭 Trace 后不会注册 Servlet 过滤器。
     */
    @Test
    void shouldSkipServletTraceFilterWhenTraceDisabled() {
        servletRunner
                .withPropertyValues("getboot.observability.trace.enabled=false")
                .run(context -> assertFalse(context.containsBean("traceMdcFilter")));
    }

    /**
     * 验证 Reactive 场景会注册 WebFilter 与 Reactor 初始化器。
     */
    @Test
    void shouldRegisterReactiveTraceFilterAndReactorInitializer() {
        reactiveRunner.run(context -> {
            assertInstanceOf(WebFilter.class, context.getBean("traceWebFilter"));
            assertInstanceOf(ReactorContextPropagationInitializer.class,
                    context.getBean(ReactorContextPropagationInitializer.class));
        });
    }

    /**
     * 验证关闭 Reactor 自动传播后仍保留 WebFilter，但不再注册初始化器。
     */
    @Test
    void shouldSkipReactorInitializerWhenAutomaticPropagationDisabled() {
        reactiveRunner
                .withPropertyValues("getboot.observability.reactor.automatic-context-propagation-enabled=false")
                .run(context -> {
                    assertTrue(context.containsBean("traceWebFilter"));
                    assertFalse(context.containsBean("reactorContextPropagationInitializer"));
                });
    }
}
