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
package com.getboot.mq.api.producer;

import com.getboot.mq.api.message.MqMessage;
import com.getboot.mq.api.model.MqSendReceipt;
import com.getboot.mq.api.model.MqTransactionReceipt;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MQ 消息生产能力接口。
 *
 * <p>对外提供稳定的消息发送入口，具体技术栈由基础设施层实现。</p>
 *
 * @author qiheng
 */
public interface MqMessageProducer {

    /**
     * 按主题和标签发送消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @return 发送回执
     * @param <T> 消息类型
     */
    <T extends MqMessage> MqSendReceipt send(String topic, String tag, T message);

    /**
     * 按逻辑目标地址发送消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @return 发送回执
     * @param <T> 消息类型
     */
    <T extends MqMessage> MqSendReceipt send(String destination, T message);

    /**
     * 按主题和标签异步发送消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @return 异步发送结果
     * @param <T> 消息类型
     */
    <T extends MqMessage> CompletableFuture<MqSendReceipt> asyncSend(String topic, String tag, T message);

    /**
     * 按逻辑目标地址异步发送消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @return 异步发送结果
     * @param <T> 消息类型
     */
    <T extends MqMessage> CompletableFuture<MqSendReceipt> asyncSend(String destination, T message);

    /**
     * 按主题和标签发送延迟消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @param delayLevel 延迟级别
     * @return 发送回执
     * @param <T> 消息类型
     */
    <T extends MqMessage> MqSendReceipt sendWithDelay(String topic, String tag, T message, int delayLevel);

    /**
     * 按逻辑目标地址发送延迟消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @param delayLevel 延迟级别
     * @return 发送回执
     * @param <T> 消息类型
     */
    <T extends MqMessage> MqSendReceipt sendWithDelay(String destination, T message, int delayLevel);

    /**
     * 按主题和标签批量发送消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param messages 消息列表
     * @return 最后一条消息的发送回执
     * @param <T> 消息类型
     */
    <T extends MqMessage> MqSendReceipt sendBatch(String topic, String tag, List<T> messages);

    /**
     * 按主题和标签发送顺序消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @param hashKey 顺序键
     * @return 发送回执
     * @param <T> 消息类型
     */
    <T extends MqMessage> MqSendReceipt sendOrderly(String topic, String tag, T message, String hashKey);

    /**
     * 按逻辑目标地址发送顺序消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @param hashKey 顺序键
     * @return 发送回执
     * @param <T> 消息类型
     */
    <T extends MqMessage> MqSendReceipt sendOrderly(String destination, T message, String hashKey);

    /**
     * 按逻辑目标地址发送事务消息。
     *
     * @param destination 逻辑目标地址
     * @param message 消息体
     * @param arg 事务参数
     * @return 事务消息回执
     * @param <T> 消息类型
     */
    <T extends MqMessage> MqTransactionReceipt sendTransaction(String destination, T message, Object arg);

    /**
     * 按主题和标签发送事务消息。
     *
     * @param topic 消息主题
     * @param tag 消息标签
     * @param message 消息体
     * @param arg 事务参数
     * @return 事务消息回执
     * @param <T> 消息类型
     */
    <T extends MqMessage> MqTransactionReceipt sendTransaction(String topic, String tag, T message, Object arg);
}
