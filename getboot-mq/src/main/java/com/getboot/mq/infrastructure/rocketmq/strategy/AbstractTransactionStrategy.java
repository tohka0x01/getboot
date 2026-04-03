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
package com.getboot.mq.infrastructure.rocketmq.strategy;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.getboot.mq.spi.rocketmq.TopicTransactionStrategy;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;

/**
 * Topic 事务策略抽象基类。
 *
 * <p>统一负责将 RocketMQ 消息负载转换为 JSON，便于子类聚焦事务业务逻辑。</p>
 *
 * @author qiheng
 */
public abstract class AbstractTransactionStrategy implements TopicTransactionStrategy {

    /**
     * 执行本地事务并将消息负载转换为 JSON。
     *
     * @param arg RocketMQ 回调参数
     * @return 本地事务执行结果
     */
    @Override
    public RocketMQLocalTransactionState executeTransaction(Object arg) {
        JSONObject jsonObject = parseMessageToJson(arg);
        return doExecuteTransaction(jsonObject);
    }

    /**
     * 执行事务回查并将消息负载转换为 JSON。
     *
     * @param arg RocketMQ 回调参数
     * @return 事务回查结果
     */
    @Override
    public RocketMQLocalTransactionState checkTransaction(Object arg) {
        JSONObject jsonObject = parseMessageToJson(arg);
        return doCheckTransaction(jsonObject);
    }

    /**
     * 将 RocketMQ 消息参数解析为 JSON 对象。
     *
     * @param msg RocketMQ 回调参数
     * @return JSON 对象
     */
    private JSONObject parseMessageToJson(Object msg) {
        Message<?> message = (Message<?>) msg;
        Object payload = message.getPayload();
        if (payload instanceof byte[] payloadBytes) {
            return JSON.parseObject(new String(payloadBytes, StandardCharsets.UTF_8));
        }
        return JSON.parseObject(JSON.toJSONString(payload));
    }

    /**
     * 执行子类定义的本地事务逻辑。
     *
     * @param jsonObject 消息负载 JSON
     * @return 本地事务执行结果
     */
    protected abstract RocketMQLocalTransactionState doExecuteTransaction(JSONObject jsonObject);

    /**
     * 执行子类定义的事务回查逻辑。
     *
     * @param jsonObject 消息负载 JSON
     * @return 事务回查结果
     */
    protected abstract RocketMQLocalTransactionState doCheckTransaction(JSONObject jsonObject);
}
