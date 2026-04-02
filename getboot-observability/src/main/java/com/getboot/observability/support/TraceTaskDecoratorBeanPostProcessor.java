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
package com.getboot.observability.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.function.Supplier;

/**
 * Trace 任务装饰器后处理器。
 *
 * <p>用于为 Spring 常见异步执行器自动挂载 Trace 任务装饰器。</p>
 *
 * @author qiheng
 */
public class TraceTaskDecoratorBeanPostProcessor implements BeanPostProcessor {

    /**
     * 任务装饰器提供器。
     */
    private final Supplier<TaskDecorator> taskDecoratorSupplier;

    /**
     * 使用固定任务装饰器创建后处理器。
     *
     * @param taskDecorator 任务装饰器
     */
    public TraceTaskDecoratorBeanPostProcessor(TaskDecorator taskDecorator) {
        this(() -> taskDecorator);
    }

    /**
     * 使用任务装饰器提供器创建后处理器。
     *
     * @param taskDecoratorSupplier 任务装饰器提供器
     */
    public TraceTaskDecoratorBeanPostProcessor(Supplier<TaskDecorator> taskDecoratorSupplier) {
        this.taskDecoratorSupplier = taskDecoratorSupplier;
    }

    /**
     * 为常见异步执行器补充 Trace 任务装饰器。
     *
     * @param bean 当前 Bean
     * @param beanName Bean 名称
     * @return 处理后的 Bean
     * @throws BeansException Bean 处理异常
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ThreadPoolTaskExecutor taskExecutor && !hasTaskDecorator(taskExecutor, ThreadPoolTaskExecutor.class)) {
            taskExecutor.setTaskDecorator(taskDecoratorSupplier.get());
            return bean;
        }
        if (bean instanceof SimpleAsyncTaskExecutor taskExecutor && !hasTaskDecorator(taskExecutor, SimpleAsyncTaskExecutor.class)) {
            taskExecutor.setTaskDecorator(taskDecoratorSupplier.get());
            return bean;
        }
        return bean;
    }

    /**
     * 判断目标执行器是否已经配置任务装饰器。
     *
     * @param target 目标执行器
     * @param targetClass 目标类型
     * @return 已配置时返回 {@code true}
     */
    private boolean hasTaskDecorator(Object target, Class<?> targetClass) {
        Field field = ReflectionUtils.findField(targetClass, "taskDecorator");
        if (field == null) {
            return false;
        }
        ReflectionUtils.makeAccessible(field);
        Object decorator = ReflectionUtils.getField(field, target);
        return decorator != null;
    }
}
