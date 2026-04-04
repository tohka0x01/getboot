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
package com.getboot.wechat.infrastructure.autoconfigure;

import cn.binarywang.wx.miniapp.api.WxMaService;
import com.getboot.wechat.api.miniapp.WechatMiniProgramNativeServices;
import com.getboot.wechat.api.officialaccount.WechatOfficialAccountNativeServices;
import me.chanjar.weixin.common.redis.RedisTemplateWxRedisOps;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import me.chanjar.weixin.mp.config.impl.WxMpRedisConfigImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 微信自动配置测试。
 *
 * @author qiheng
 */
class WechatAutoConfigurationTest {

    /**
     * 测试用上下文运行器。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WechatAutoConfiguration.class))
            .withPropertyValues(
                    "getboot.wechat.mini-program.apps.wx-mini-a=mini-secret-a",
                    "getboot.wechat.official-account.apps.wx-mp-a=mp-secret-a"
            );

    /**
     * 验证存在 StringRedisTemplate 时会注册 Redis 适配器，并为服务号启用 Redis 配置。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldRegisterWechatBeansWithRedisSupportWhenStringRedisTemplatePresent() {
        contextRunner
                .withBean(
                        StringRedisTemplate.class,
                        () -> new TestStringRedisTemplate(createConnectionFactory())
                )
                .run(context -> {
                    assertTrue(context.containsBean("redisTemplateWxRedisOps"));
                    assertTrue(context.containsBean("wxMaServices"));
                    assertTrue(context.containsBean("wxMpServices"));
                    assertInstanceOf(RedisTemplateWxRedisOps.class, context.getBean(RedisTemplateWxRedisOps.class));
                    assertInstanceOf(WechatMiniProgramNativeServices.class, context.getBean(WechatMiniProgramNativeServices.class));
                    assertInstanceOf(
                            WechatOfficialAccountNativeServices.class,
                            context.getBean(WechatOfficialAccountNativeServices.class)
                    );

                    Map<String, WxMaService> wxMaServices = context.getBean("wxMaServices", Map.class);
                    Map<String, WxMpService> wxMpServices = context.getBean("wxMpServices", Map.class);
                    assertEquals(1, wxMaServices.size());
                    assertEquals(1, wxMpServices.size());
                    assertInstanceOf(
                            WxMpRedisConfigImpl.class,
                            ((WxMpService) wxMpServices.get("wx-mp-a")).getWxMpConfigStorage()
                    );
                });
    }

    /**
     * 验证缺少 StringRedisTemplate 时不注册 Redis 适配器，并回退到默认服务号配置。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldFallbackToDefaultOfficialAccountStorageWhenRedisTemplateMissing() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("redisTemplateWxRedisOps"));

            Map<String, WxMpService> wxMpServices = context.getBean("wxMpServices", Map.class);
            assertEquals(1, wxMpServices.size());
            assertInstanceOf(
                    WxMpDefaultConfigImpl.class,
                    ((WxMpService) wxMpServices.get("wx-mp-a")).getWxMpConfigStorage()
            );
        });
    }

    /**
     * 测试用字符串 RedisTemplate，避免真实初始化外部连接。
     */
    private static final class TestStringRedisTemplate extends StringRedisTemplate {

        /**
         * 创建测试用字符串模板。
         *
         * @param connectionFactory Redis 连接工厂
         */
        private TestStringRedisTemplate(RedisConnectionFactory connectionFactory) {
            setConnectionFactory(connectionFactory);
        }

        /**
         * 跳过真实初始化。
         */
        @Override
        public void afterPropertiesSet() {
        }
    }

    /**
     * 创建测试用 Redis 连接工厂代理。
     *
     * @return Redis 连接工厂
     */
    private static RedisConnectionFactory createConnectionFactory() {
        return (RedisConnectionFactory) Proxy.newProxyInstance(
                WechatAutoConfigurationTest.class.getClassLoader(),
                new Class<?>[]{RedisConnectionFactory.class},
                (proxy, method, args) -> null
        );
    }
}
