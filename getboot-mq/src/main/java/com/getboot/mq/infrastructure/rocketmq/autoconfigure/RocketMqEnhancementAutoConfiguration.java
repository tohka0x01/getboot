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
package com.getboot.mq.infrastructure.rocketmq.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.getboot.mq.api.producer.MqMessageProducer;
import com.getboot.mq.api.properties.MqProperties;
import com.getboot.mq.api.properties.MqTraceProperties;
import com.getboot.mq.infrastructure.rocketmq.aop.RocketMqTraceListenerAspect;
import com.getboot.mq.infrastructure.rocketmq.listener.TopicRoutingTransactionListener;
import com.getboot.mq.infrastructure.rocketmq.producer.RocketMqMessageProducer;
import com.getboot.mq.spi.MqMessageHeadersCustomizer;
import com.getboot.mq.spi.rocketmq.RocketMQMessageConverterCustomizer;
import com.getboot.mq.spi.rocketmq.TopicTransactionStrategy;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

import java.util.List;

/**
 * RocketMQ 增强自动配置。
 *
 * <p>负责注册 RocketMQ 消息生产实现、事务监听器、Trace 切面以及消息转换器增强。</p>
 *
 * @author qiheng
 */
@AutoConfiguration
@ConditionalOnClass(RocketMQTemplate.class)
@EnableConfigurationProperties({MqProperties.class, MqTraceProperties.class})
@ConditionalOnProperty(prefix = "getboot.mq", name = "enabled", havingValue = "true")
@ConditionalOnExpression("'${getboot.mq.type:rocketmq}' == 'rocketmq' and '${getboot.mq.rocketmq.enabled:true}' == 'true'")
public class RocketMqEnhancementAutoConfiguration {

    /**
     * 注册 RocketMQ 消息生产者实现。
     *
     * @param rocketMQTemplate RocketMQ 模板
     * @param traceProperties MQ Trace 配置
     * @param messageHeadersCustomizers 消息头定制器集合
     * @return MQ 消息生产者
     */
    @Bean
    @ConditionalOnMissingBean
    public MqMessageProducer mqMessageProducer(
            RocketMQTemplate rocketMQTemplate,
            MqTraceProperties traceProperties,
            ObjectProvider<MqMessageHeadersCustomizer> messageHeadersCustomizers) {
        return new RocketMqMessageProducer(
                rocketMQTemplate,
                traceProperties,
                messageHeadersCustomizers.orderedStream().toList()
        );
    }

    /**
     * 注册增强版 RocketMQ 消息转换器。
     *
     * @param converterCustomizers RocketMQ 消息转换器定制器集合
     * @return 增强版 RocketMQ 消息转换器
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(RocketMQMessageConverter.class)
    public RocketMQMessageConverter enhanceRocketMQMessageConverter(
            ObjectProvider<RocketMQMessageConverterCustomizer> converterCustomizers) {
        RocketMQMessageConverter converter = new RocketMQMessageConverter();
        CompositeMessageConverter compositeMessageConverter = (CompositeMessageConverter) converter.getMessageConverter();
        List<MessageConverter> messageConverterList = compositeMessageConverter.getConverters();
        for (MessageConverter messageConverter : messageConverterList) {
            if (messageConverter instanceof MappingJackson2MessageConverter jackson2MessageConverter) {
                ObjectMapper objectMapper = jackson2MessageConverter.getObjectMapper();
                objectMapper.registerModules(new JavaTimeModule());
                converterCustomizers.orderedStream().forEach(customizer -> customizer.customize(jackson2MessageConverter));
            }
        }
        return converter;
    }

    /**
     * 注册按 Topic 路由的事务监听器。
     *
     * @param strategies Topic 事务策略集合
     * @param traceProperties MQ Trace 配置
     * @return 事务监听器
     */
    @Bean
    @ConditionalOnMissingBean
    public TopicRoutingTransactionListener topicRoutingTransactionListener(
            List<TopicTransactionStrategy> strategies,
            MqTraceProperties traceProperties) {
        return new TopicRoutingTransactionListener(strategies, traceProperties);
    }

    /**
     * 注册 RocketMQ Trace 监听切面。
     *
     * @param traceProperties MQ Trace 配置
     * @return RocketMQ Trace 监听切面
     */
    @Bean
    @ConditionalOnMissingBean
    public RocketMqTraceListenerAspect rocketMqTraceListenerAspect(MqTraceProperties traceProperties) {
        return new RocketMqTraceListenerAspect(traceProperties);
    }
}
