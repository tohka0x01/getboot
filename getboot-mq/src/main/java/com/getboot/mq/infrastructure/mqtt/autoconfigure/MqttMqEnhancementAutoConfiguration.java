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
import com.getboot.mq.infrastructure.mqtt.support.NettyMqttPublishingGateway;
import com.getboot.mq.spi.MqMessageHeadersCustomizer;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MQTT MQ 增强自动配置。
 *
 * <p>负责注册 Netty MQTT 发布网关以及统一消息发送门面。</p>
 *
 * @author qiheng
 */
@AutoConfiguration
@ConditionalOnClass(MqttMessageBuilders.class)
@EnableConfigurationProperties({MqProperties.class, MqTraceProperties.class})
@ConditionalOnProperty(prefix = "getboot.mq", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${getboot.mq.type:rocketmq}' == 'mqtt' and '${getboot.mq.mqtt.enabled:false}' == 'true'")
public class MqttMqEnhancementAutoConfiguration {

    /**
     * 注册 Netty MQTT 发布网关。
     *
     * @param mqProperties MQ 模块配置
     * @return MQTT 发布网关
     */
    @Bean
    @ConditionalOnMissingBean
    public NettyMqttPublishingGateway nettyMqttPublishingGateway(MqProperties mqProperties) {
        return new NettyMqttPublishingGateway(mqProperties.getMqtt());
    }

    /**
     * 注册 MQTT 消息生产者实现。
     *
     * @param publishingGateway MQTT 发布网关
     * @param mqProperties MQ 模块配置
     * @param traceProperties MQ Trace 配置
     * @param messageHeadersCustomizers 消息头定制器集合
     * @return MQ 消息生产者
     */
    @Bean
    @ConditionalOnMissingBean
    public MqMessageProducer mqMessageProducer(
            NettyMqttPublishingGateway publishingGateway,
            MqProperties mqProperties,
            MqTraceProperties traceProperties,
            ObjectProvider<MqMessageHeadersCustomizer> messageHeadersCustomizers) {
        return new MqttMqMessageProducer(
                publishingGateway,
                mqProperties.getMqtt(),
                traceProperties,
                messageHeadersCustomizers.orderedStream().toList()
        );
    }
}
