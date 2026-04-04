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
import com.getboot.mq.api.properties.MqProperties;
import com.getboot.mq.api.properties.MqTraceProperties;
import com.getboot.mq.infrastructure.mqtt.producer.MqttMqMessageProducer;
import com.getboot.mq.spi.MqMessageHeadersCustomizer;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StringUtils;

/**
 * MQTT MQ 增强自动配置。
 *
 * <p>负责注册 Spring Integration MQTT 出站处理器以及统一消息发送门面。</p>
 *
 * @author qiheng
 */
@AutoConfiguration
@ConditionalOnClass(MqttPahoMessageHandler.class)
@EnableConfigurationProperties({MqProperties.class, MqTraceProperties.class})
@ConditionalOnProperty(prefix = "getboot.mq", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${getboot.mq.type:rocketmq}' == 'mqtt' and '${getboot.mq.mqtt.enabled:false}' == 'true'")
public class MqttMqEnhancementAutoConfiguration {

    /**
     * 注册默认 MQTT Paho 客户端工厂。
     *
     * @param mqProperties MQ 模块配置
     * @return MQTT 客户端工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public MqttPahoClientFactory mqttPahoClientFactory(MqProperties mqProperties) {
        MqProperties.Mqtt mqttProperties = mqProperties.getMqtt();
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setServerURIs(new String[]{mqttProperties.getServerUri().trim()});
        connectOptions.setCleanSession(true);
        connectOptions.setConnectionTimeout(resolveConnectionTimeoutSeconds(mqttProperties.getConnectTimeoutMs()));
        connectOptions.setKeepAliveInterval(mqttProperties.getKeepAliveSeconds());
        if (StringUtils.hasText(mqttProperties.getUsername())) {
            connectOptions.setUserName(mqttProperties.getUsername().trim());
        }
        if (StringUtils.hasText(mqttProperties.getPassword())) {
            connectOptions.setPassword(mqttProperties.getPassword().toCharArray());
        }
        DefaultMqttPahoClientFactory clientFactory = new DefaultMqttPahoClientFactory();
        clientFactory.setConnectionOptions(connectOptions);
        return clientFactory;
    }

    /**
     * 注册 MQTT 出站消息处理器。
     *
     * @param mqProperties MQ 模块配置
     * @param clientFactory MQTT 客户端工厂
     * @return 出站消息处理器
     */
    @Bean
    @ConditionalOnMissingBean(name = "mqttOutboundMessageHandler")
    public MessageHandler mqttOutboundMessageHandler(
            MqProperties mqProperties,
            MqttPahoClientFactory clientFactory) {
        MqProperties.Mqtt mqttProperties = mqProperties.getMqtt();
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(resolveClientId(mqttProperties), clientFactory);
        messageHandler.setDefaultQos(mqttProperties.getDefaultQos());
        messageHandler.setDefaultRetained(mqttProperties.isRetained());
        messageHandler.setAsync(mqttProperties.isAsync());
        return messageHandler;
    }

    /**
     * 注册 MQTT 消息生产者实现。
     *
     * @param mqttOutboundMessageHandler MQTT 出站消息处理器
     * @param mqProperties MQ 模块配置
     * @param traceProperties MQ Trace 配置
     * @param messageHeadersCustomizers 消息头定制器集合
     * @return MQ 消息生产者
     */
    @Bean
    @ConditionalOnMissingBean
    public MqMessageProducer mqMessageProducer(
            @Qualifier("mqttOutboundMessageHandler") MessageHandler mqttOutboundMessageHandler,
            MqProperties mqProperties,
            MqTraceProperties traceProperties,
            ObjectProvider<MqMessageHeadersCustomizer> messageHeadersCustomizers) {
        return new MqttMqMessageProducer(
                mqttOutboundMessageHandler,
                mqProperties.getMqtt(),
                traceProperties,
                messageHeadersCustomizers.orderedStream().toList()
        );
    }

    /**
     * 将毫秒级连接超时换算为 Paho 所需的秒级配置。
     *
     * @param connectTimeoutMs 连接超时毫秒数
     * @return 连接超时秒数
     */
    private int resolveConnectionTimeoutSeconds(int connectTimeoutMs) {
        if (connectTimeoutMs <= 0) {
            return 1;
        }
        return Math.max(1, (connectTimeoutMs + 999) / 1000);
    }

    /**
     * 解析 MQTT 客户端标识。
     *
     * @param mqttProperties MQTT 配置
     * @return 客户端标识
     */
    private String resolveClientId(MqProperties.Mqtt mqttProperties) {
        if (StringUtils.hasText(mqttProperties.getClientId())) {
            return mqttProperties.getClientId().trim();
        }
        return "getboot-mqtt";
    }
}
