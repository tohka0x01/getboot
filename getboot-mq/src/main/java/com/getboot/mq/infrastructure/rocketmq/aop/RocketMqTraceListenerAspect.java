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
package com.getboot.mq.infrastructure.rocketmq.aop;

import com.getboot.mq.api.properties.MqTraceProperties;
import com.getboot.mq.support.MqTraceContextSupport;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * RocketMQ Trace 监听切面。
 *
 * <p>用于在消息消费与事务回查时自动恢复 Trace 上下文，保障日志链路连续。</p>
 *
 * @author qiheng
 */
@Aspect
public class RocketMqTraceListenerAspect {

    /**
     * MQ Trace 上下文支撑工具。
     */
    private final MqTraceContextSupport traceContextSupport;

    /**
     * 创建 RocketMQ Trace 监听切面。
     *
     * @param traceProperties MQ Trace 配置
     */
    public RocketMqTraceListenerAspect(MqTraceProperties traceProperties) {
        this.traceContextSupport = new MqTraceContextSupport(traceProperties);
    }

    /**
     * 在 RocketMQ 普通监听器执行前恢复 Trace 上下文。
     *
     * @param joinPoint 切点信息
     * @return 监听器执行结果
     * @throws Throwable 监听器抛出的异常
     */
    @Around("this(org.apache.rocketmq.spring.core.RocketMQListener) && execution(* *.onMessage(..))")
    public Object aroundRocketMqListener(ProceedingJoinPoint joinPoint) throws Throwable {
        return proceedWithTrace(joinPoint);
    }

    /**
     * 在 RocketMQ 事务监听器执行前恢复 Trace 上下文。
     *
     * @param joinPoint 切点信息
     * @return 事务监听器执行结果
     * @throws Throwable 事务监听器抛出的异常
     */
    @Around("this(org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener) && "
            + "(execution(* *.executeLocalTransaction(..)) || execution(* *.checkLocalTransaction(..)))")
    public Object aroundRocketMqLocalTransactionListener(ProceedingJoinPoint joinPoint) throws Throwable {
        return proceedWithTrace(joinPoint);
    }

    /**
     * 在目标方法执行前打开 Trace 作用域。
     *
     * @param joinPoint 切点信息
     * @return 目标方法执行结果
     * @throws Throwable 目标方法抛出的异常
     */
    private Object proceedWithTrace(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!traceContextSupport.isEnabled()) {
            return joinPoint.proceed();
        }
        String traceId = resolveTraceId(joinPoint.getArgs());
        try (MqTraceContextSupport.TraceScope ignored = traceContextSupport.openScope(traceId)) {
            return joinPoint.proceed();
        }
    }

    /**
     * 从方法参数中解析 TraceId。
     *
     * @param arguments 方法参数
     * @return 解析到的 TraceId
     */
    private String resolveTraceId(Object[] arguments) {
        if (arguments == null) {
            return null;
        }
        for (Object argument : arguments) {
            String traceId = traceContextSupport.resolveInboundTraceId(argument);
            if (traceId != null) {
                return traceId;
            }
        }
        return null;
    }
}
