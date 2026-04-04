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
package com.getboot.websocket.infrastructure.jakarta.autoconfigure;

import com.getboot.websocket.api.registry.WebSocketSessionRegistry;
import com.getboot.websocket.api.sender.WebSocketMessageSender;
import com.getboot.websocket.infrastructure.jakarta.endpoint.GetbootWebSocketEndpoint;
import com.getboot.websocket.infrastructure.jakarta.endpoint.GetbootWebSocketEndpointConfigurator;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;
import com.getboot.websocket.spi.WebSocketUserIdResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jakarta WebSocket 自动配置测试。
 *
 * @author qiheng
 */
class JakartaWebSocketAutoConfigurationTest {

    /**
     * 上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JakartaWebSocketAutoConfiguration.class))
            .withPropertyValues("getboot.websocket.enabled=true");

    /**
     * 验证默认自动配置会注册 WebSocket 核心 Bean。
     */
    @Test
    void shouldRegisterDefaultWebSocketBeans() {
        contextRunner.run(context -> {
            assertInstanceOf(WebSocketSessionRegistry.class, context.getBean(WebSocketSessionRegistry.class));
            assertInstanceOf(WebSocketMessageSender.class, context.getBean(WebSocketMessageSender.class));
            assertInstanceOf(WebSocketUserIdResolver.class, context.getBean(WebSocketUserIdResolver.class));
            assertInstanceOf(GetbootWebSocketEndpoint.class, context.getBean(GetbootWebSocketEndpoint.class));
            assertInstanceOf(GetbootWebSocketEndpointConfigurator.class,
                    context.getBean(GetbootWebSocketEndpointConfigurator.class));
            assertTrue(context.containsBean("getbootWebSocketEndpointInitializer"));
            assertInstanceOf(ServletContextInitializer.class, context.getBean("getbootWebSocketEndpointInitializer"));
        });
    }

    /**
     * 验证关闭模块后不会注册默认 Bean。
     */
    @Test
    void shouldSkipAllBeansWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JakartaWebSocketAutoConfiguration.class))
                .withPropertyValues("getboot.websocket.enabled=false")
                .run(context -> {
                    assertFalse(context.containsBean("webSocketSessionRegistry"));
                    assertFalse(context.containsBean("webSocketMessageSender"));
                    assertFalse(context.containsBean("webSocketUserIdResolver"));
                    assertFalse(context.containsBean("getbootWebSocketEndpoint"));
                });
    }

    /**
     * 验证业务方自定义 Bean 存在时，自动配置会正常让位。
     */
    @Test
    void shouldBackOffWhenCustomBeansProvided() {
        WebSocketSessionRegistry customRegistry = new com.getboot.websocket.support.registry.DefaultWebSocketSessionRegistry();
        WebSocketMessageSender customSender = new WebSocketMessageSender() {
            @Override
            public int sendToSession(String sessionId, Object payload) {
                return 0;
            }

            @Override
            public int sendToUser(String userId, Object payload) {
                return 0;
            }

            @Override
            public int broadcast(Object payload) {
                return 0;
            }
        };

        contextRunner
                .withBean(WebSocketSessionRegistry.class, () -> customRegistry)
                .withBean(WebSocketMessageSender.class, () -> customSender)
                .run(context -> {
                    assertSame(customRegistry, context.getBean(WebSocketSessionRegistry.class));
                    assertSame(customSender, context.getBean(WebSocketMessageSender.class));
                });
    }

    /**
     * 验证业务方自定义 Endpoint 配置器存在时，自动配置会复用该配置器。
     */
    @Test
    void shouldReuseCustomEndpointConfigurator() {
        Configurator customConfigurator = new Configurator() {
        };

        contextRunner
                .withBean(Configurator.class, () -> customConfigurator)
                .run(context -> {
                    assertSame(customConfigurator, context.getBean(Configurator.class));
                    assertFalse(context.containsBean("getbootWebSocketEndpointConfigurator"));
                    assertTrue(context.containsBean("getbootWebSocketEndpointInitializer"));
                    assertInstanceOf(ServletContextInitializer.class, context.getBean("getbootWebSocketEndpointInitializer"));
                });
    }
}
