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

import com.getboot.websocket.api.properties.WebSocketProperties;
import com.getboot.websocket.api.registry.WebSocketSessionRegistry;
import com.getboot.websocket.api.sender.WebSocketMessageSender;
import com.getboot.websocket.infrastructure.jakarta.endpoint.GetbootWebSocketEndpoint;
import com.getboot.websocket.infrastructure.jakarta.endpoint.GetbootWebSocketEndpointConfigurator;
import com.getboot.websocket.spi.WebSocketSessionLifecycleListener;
import com.getboot.websocket.spi.WebSocketTextMessageListener;
import com.getboot.websocket.spi.WebSocketUserIdResolver;
import com.getboot.websocket.support.registry.DefaultWebSocketSessionRegistry;
import com.getboot.websocket.support.resolver.DefaultWebSocketUserIdResolver;
import com.getboot.websocket.support.sender.DefaultWebSocketMessageSender;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

/**
 * Jakarta WebSocket 自动配置。
 *
 * @author qiheng
 */
@AutoConfiguration
@ConditionalOnClass(ServerContainer.class)
@EnableConfigurationProperties(WebSocketProperties.class)
@ConditionalOnProperty(prefix = "getboot.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JakartaWebSocketAutoConfiguration {

    /**
     * 注册默认会话注册表。
     *
     * @return 会话注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketSessionRegistry webSocketSessionRegistry() {
        return new DefaultWebSocketSessionRegistry();
    }

    /**
     * 注册默认用户标识解析器。
     *
     * @param properties WebSocket 模块配置
     * @return 用户标识解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketUserIdResolver webSocketUserIdResolver(WebSocketProperties properties) {
        return new DefaultWebSocketUserIdResolver(properties);
    }

    /**
     * 注册默认消息推送器。
     *
     * @param sessionRegistry 会话注册表
     * @return 消息推送器
     */
    @Bean
    @ConditionalOnMissingBean
    public WebSocketMessageSender webSocketMessageSender(WebSocketSessionRegistry sessionRegistry) {
        return new DefaultWebSocketMessageSender(sessionRegistry);
    }

    /**
     * 注册默认 Endpoint。
     *
     * @param sessionRegistry 会话注册表
     * @param properties WebSocket 模块配置
     * @param userIdResolver 用户标识解析器
     * @param lifecycleListeners 生命周期监听器集合
     * @param textMessageListeners 文本消息监听器集合
     * @return Endpoint
     */
    @Bean
    @ConditionalOnMissingBean
    public GetbootWebSocketEndpoint getbootWebSocketEndpoint(
            WebSocketSessionRegistry sessionRegistry,
            WebSocketProperties properties,
            WebSocketUserIdResolver userIdResolver,
            ObjectProvider<WebSocketSessionLifecycleListener> lifecycleListeners,
            ObjectProvider<WebSocketTextMessageListener> textMessageListeners) {
        return new GetbootWebSocketEndpoint(
                sessionRegistry,
                properties,
                userIdResolver,
                lifecycleListeners.orderedStream().toList(),
                textMessageListeners.orderedStream().toList()
        );
    }

    /**
     * 注册默认 Endpoint 配置器。
     *
     * @param endpoint Endpoint
     * @param properties WebSocket 模块配置
     * @return Endpoint 配置器
     */
    @Bean
    @ConditionalOnMissingBean(Configurator.class)
    public GetbootWebSocketEndpointConfigurator getbootWebSocketEndpointConfigurator(
            GetbootWebSocketEndpoint endpoint,
            WebSocketProperties properties) {
        return new GetbootWebSocketEndpointConfigurator(endpoint, properties);
    }

    /**
     * 注册 Endpoint 到 Servlet 容器。
     *
     * @param properties WebSocket 模块配置
     * @param configurator Endpoint 配置器
     * @return Servlet 上下文初始化器
     */
    @Bean
    @ConditionalOnMissingBean(name = "getbootWebSocketEndpointInitializer")
    public ServletContextInitializer getbootWebSocketEndpointInitializer(
            WebSocketProperties properties,
            Configurator configurator) {
        return servletContext -> {
            Object serverContainer = servletContext.getAttribute(ServerContainer.class.getName());
            Assert.state(serverContainer instanceof ServerContainer, "Servlet container does not expose ServerContainer.");
            ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder
                    .create(GetbootWebSocketEndpoint.class, properties.getEndpoint())
                    .configurator(configurator)
                    .build();
            try {
                ((ServerContainer) serverContainer).addEndpoint(endpointConfig);
            } catch (DeploymentException ex) {
                throw new IllegalStateException("Failed to register Getboot WebSocket endpoint.", ex);
            }
        };
    }
}
