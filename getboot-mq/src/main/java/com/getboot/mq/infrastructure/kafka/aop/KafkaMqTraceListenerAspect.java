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
package com.getboot.mq.infrastructure.kafka.aop;

import com.getboot.mq.api.properties.MqTraceProperties;
import com.getboot.mq.support.MqTraceContextSupport;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Kafka Trace 监听切面。
 *
 * <p>用于在 {@code @KafkaListener} 方法执行前恢复 Trace 上下文。</p>
 *
 * @author qiheng
 */
@Aspect
public class KafkaMqTraceListenerAspect {

    /**
     * MQ Trace 上下文支撑工具。
     */
    private final MqTraceContextSupport traceContextSupport;

    /**
     * 创建 Kafka Trace 监听切面。
     *
     * @param traceProperties MQ Trace 配置
     */
    public KafkaMqTraceListenerAspect(MqTraceProperties traceProperties) {
        this.traceContextSupport = new MqTraceContextSupport(traceProperties);
    }

    /**
     * 在 Kafka 监听方法执行前恢复 Trace 上下文。
     *
     * @param joinPoint 切点信息
     * @return 监听方法执行结果
     * @throws Throwable 监听方法抛出的异常
     */
    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener) || "
            + "@within(org.springframework.kafka.annotation.KafkaListener)")
    public Object aroundKafkaListener(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!traceContextSupport.isEnabled()) {
            return joinPoint.proceed();
        }
        String traceId = resolveTraceId(joinPoint.getArgs());
        try (MqTraceContextSupport.TraceScope ignored = traceContextSupport.openScope(traceId)) {
            return joinPoint.proceed();
        }
    }

    /**
     * 从监听方法参数中解析 TraceId。
     *
     * @param arguments 监听方法参数
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
