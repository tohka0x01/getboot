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
package com.getboot.job.infrastructure.xxl.autoconfigure;

import com.getboot.job.api.properties.JobProperties;
import com.getboot.job.infrastructure.xxl.client.XxlJobAdminClient;
import com.getboot.job.spi.xxl.XxlJobAdminClientConfigurer;
import com.getboot.job.spi.xxl.XxlJobExecutorCustomizer;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * XXL-JOB 自动配置测试。
 *
 * @author qiheng
 */
class XxlJobAutoConfigurationTest {

    /**
     * 测试用上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(XxlJobAutoConfiguration.class));

    /**
     * 验证任务模块开启时会注册执行器与管理端客户端，并允许定制最终配置。
     */
    @Test
    void shouldRegisterExecutorAndAdminClientWhenJobEnabled() {
        JobProperties jobProperties = createJobProperties();
        XxlJobAutoConfiguration autoConfiguration = new XxlJobAutoConfiguration(jobProperties);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean(
                "executorCustomizer",
                (XxlJobExecutorCustomizer) executor -> executor.setAddress("http://executor.demo")
        );
        beanFactory.addBean(
                "adminClientConfigurer",
                (XxlJobAdminClientConfigurer) configuration -> configuration.setUsername("custom-admin")
        );

        XxlJobSpringExecutor executor =
                autoConfiguration.xxlJobExecutor(beanFactory.getBeanProvider(XxlJobExecutorCustomizer.class));
        XxlJobAdminClient adminClient =
                autoConfiguration.xxlJobAdminClient(beanFactory.getBeanProvider(XxlJobAdminClientConfigurer.class));

        assertEquals(
                "http://127.0.0.1:8080/xxl-job-admin/,http://127.0.0.1:8081/xxl-job-admin",
                readField(executor, XxlJobExecutor.class, "adminAddresses")
        );
        assertEquals("demo-job", readField(executor, XxlJobExecutor.class, "appname"));
        assertEquals("http://executor.demo", readField(executor, XxlJobExecutor.class, "address"));
        assertEquals("127.0.0.1", readField(executor, XxlJobExecutor.class, "ip"));
        assertEquals(9999, readField(executor, XxlJobExecutor.class, "port"));

        assertEquals("http://127.0.0.1:8080/xxl-job-admin", readField(adminClient, XxlJobAdminClient.class, "adminUrl"));
        assertEquals("custom-admin", readField(adminClient, XxlJobAdminClient.class, "username"));
        assertEquals("123456", readField(adminClient, XxlJobAdminClient.class, "password"));
        assertEquals("demo-job", readField(adminClient, XxlJobAdminClient.class, "appName"));
    }

    /**
     * 验证任务模块关闭时不会注册 XXL-JOB 相关 Bean。
     */
    @Test
    void shouldSkipExecutorAndAdminClientWhenJobDisabled() {
        contextRunner
                .withPropertyValues("getboot.job.enabled=false")
                .run(context -> {
                    assertFalse(context.containsBean("xxlJobExecutor"));
                    assertFalse(context.containsBean("xxlJobAdminClient"));
                });
    }

    /**
     * 验证管理端客户端在未初始化配置时会直接拒绝访问。
     */
    @Test
    void shouldFailWhenAdminClientUsedBeforeInitialization() {
        XxlJobAdminClient adminClient = new XxlJobAdminClient();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adminClient.getJobGroupIdByAppname("demo-job")
        );

        assertEquals(
                "XXL-JOB configuration has not been initialized. Call initConfig first.",
                exception.getMessage()
        );
    }

    /**
     * 通过反射读取字段值，便于校验第三方对象状态。
     *
     * @param target 目标对象
     * @param owner 字段声明类
     * @param fieldName 字段名
     * @return 字段值
     */
    private Object readField(Object target, Class<?> owner, String fieldName) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException("Failed to read field: " + fieldName, exception);
        }
    }

    /**
     * 构造测试用任务配置。
     *
     * @return 任务配置
     */
    private JobProperties createJobProperties() {
        JobProperties jobProperties = new JobProperties();
        jobProperties.setEnabled(true);
        jobProperties.getXxl().getAdmin().setAddresses(
                "http://127.0.0.1:8080/xxl-job-admin/,http://127.0.0.1:8081/xxl-job-admin"
        );
        jobProperties.getXxl().getAdmin().setUsername("admin");
        jobProperties.getXxl().getAdmin().setPassword("123456");
        jobProperties.getXxl().getExecutor().setAppName("demo-job");
        jobProperties.getXxl().getExecutor().setIp("127.0.0.1");
        jobProperties.getXxl().getExecutor().setPort(9999);
        jobProperties.getXxl().getExecutor().setLogPath("./logs/xxl-job");
        jobProperties.getXxl().getExecutor().setLogRetentionDays(30);
        return jobProperties;
    }
}
