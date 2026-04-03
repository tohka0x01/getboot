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
package com.getboot.mq.infrastructure.rocketmq.listener;

import com.getboot.mq.api.properties.MqTraceProperties;
import com.getboot.mq.spi.rocketmq.TopicTransactionStrategy;
import com.getboot.mq.support.MqTraceContextSupport;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;

import java.util.List;

/**
 * 按 Topic 路由的事务监听器。
 *
 * <p>根据消息头中的 Topic 选择对应事务策略，统一处理本地事务执行与回查。</p>
 *
 * @author qiheng
 */
@RocketMQTransactionListener
public class TopicRoutingTransactionListener implements RocketMQLocalTransactionListener {

    /**
     * Topic 事务策略集合。
     */
    private final List<TopicTransactionStrategy> strategies;

    /**
     * MQ Trace 上下文支撑工具。
     */
    private final MqTraceContextSupport traceContextSupport;

    /**
     * 创建按 Topic 路由的事务监听器。
     *
     * @param strategies Topic 事务策略集合
     * @param traceProperties MQ Trace 配置
     */
    public TopicRoutingTransactionListener(List<TopicTransactionStrategy> strategies,
                                           MqTraceProperties traceProperties) {
        this.strategies = strategies;
        this.traceContextSupport = new MqTraceContextSupport(traceProperties);
    }

    /**
     * 执行本地事务。
     *
     * @param msg RocketMQ 消息
     * @param arg 事务参数
     * @return 本地事务执行结果
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        return executeWithTrace(msg, () -> {
            String topic = msg.getHeaders().get(RocketMQHeaders.TOPIC, String.class);
            return strategies.stream()
                    .filter(strategy -> strategy.supports(topic))
                    .findFirst()
                    .map(strategy -> strategy.executeTransaction(msg))
                    .orElse(RocketMQLocalTransactionState.ROLLBACK);
        });
    }

    /**
     * 执行事务回查。
     *
     * @param msg RocketMQ 消息
     * @return 事务回查结果
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        return executeWithTrace(msg, () -> {
            String topic = msg.getHeaders().get(RocketMQHeaders.TOPIC, String.class);
            return strategies.stream()
                    .filter(strategy -> strategy.supports(topic))
                    .findFirst()
                    .map(strategy -> strategy.checkTransaction(msg))
                    .orElse(RocketMQLocalTransactionState.ROLLBACK);
        });
    }

    /**
     * 在事务回调中打开 Trace 作用域后再执行目标逻辑。
     *
     * @param msg RocketMQ 消息
     * @param callback 事务回调
     * @return 事务回调执行结果
     */
    private RocketMQLocalTransactionState executeWithTrace(Message msg, TransactionCallback callback) {
        String traceId = traceContextSupport.resolveInboundTraceId(msg);
        try (MqTraceContextSupport.TraceScope ignored = traceContextSupport.openScope(traceId)) {
            return callback.execute();
        }
    }

    /**
     * 事务回调接口。
     */
    @FunctionalInterface
    private interface TransactionCallback {

        /**
         * 执行事务回调。
         *
         * @return 事务执行结果
         */
        RocketMQLocalTransactionState execute();
    }
}
