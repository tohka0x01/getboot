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
package com.getboot.wechat.infrastructure.support;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.config.WxMaConfig;
import com.getboot.wechat.api.properties.WechatProperties;
import com.getboot.wechat.infrastructure.miniapp.support.WechatMiniProgramServiceFactory;
import com.getboot.wechat.infrastructure.officialaccount.support.WechatOfficialAccountServiceFactory;
import me.chanjar.weixin.common.redis.RedisTemplateWxRedisOps;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.config.WxMpConfigStorage;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import me.chanjar.weixin.mp.config.impl.WxMpRedisConfigImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 微信服务工厂测试。
 *
 * @author qiheng
 */
class WechatServiceFactoryTest {

    /**
     * 验证小程序服务工厂会按 appId 初始化原生服务。
     */
    @Test
    void shouldCreateMiniProgramServicesFromConfiguredApps() {
        WechatProperties.AppCredentialGroup appCredentialGroup = new WechatProperties.AppCredentialGroup();
        appCredentialGroup.setApps(Map.of("wx-mini-a", "mini-secret-a"));

        WechatMiniProgramServiceFactory serviceFactory = new WechatMiniProgramServiceFactory();

        Map<String, WxMaService> services = serviceFactory.createServices(appCredentialGroup);

        WxMaConfig config = services.get("wx-mini-a").getWxMaConfig();
        assertEquals(1, services.size());
        assertEquals("wx-mini-a", config.getAppid());
        assertEquals("mini-secret-a", config.getSecret());
    }

    /**
     * 验证服务号服务工厂在存在 Redis 时会使用 Redis 配置实现。
     */
    @Test
    void shouldCreateOfficialAccountServicesWithRedisStorageWhenRedisOpsProvided() {
        WechatProperties.AppCredentialGroup appCredentialGroup = new WechatProperties.AppCredentialGroup();
        appCredentialGroup.setApps(Map.of("wx-mp-a", "mp-secret-a"));
        RedisTemplateWxRedisOps redisOps = new RedisTemplateWxRedisOps(
                new TestStringRedisTemplate(createConnectionFactory())
        );

        WechatOfficialAccountServiceFactory serviceFactory = new WechatOfficialAccountServiceFactory();

        Map<String, WxMpService> services = serviceFactory.createServices(appCredentialGroup, redisOps);

        WxMpConfigStorage configStorage = services.get("wx-mp-a").getWxMpConfigStorage();
        assertEquals(1, services.size());
        assertInstanceOf(WxMpRedisConfigImpl.class, configStorage);
        assertEquals("wx-mp-a", configStorage.getAppId());
        assertEquals("mp-secret-a", configStorage.getSecret());
        assertTrue(((WxMpRedisConfigImpl) configStorage).getRedisOps() instanceof RedisTemplateWxRedisOps);
    }

    /**
     * 验证服务号服务工厂在没有 Redis 时会回退到默认内存配置。
     */
    @Test
    void shouldCreateOfficialAccountServicesWithDefaultStorageWhenRedisOpsMissing() {
        WechatProperties.AppCredentialGroup appCredentialGroup = new WechatProperties.AppCredentialGroup();
        appCredentialGroup.setApps(Map.of("wx-mp-a", "mp-secret-a"));

        WechatOfficialAccountServiceFactory serviceFactory = new WechatOfficialAccountServiceFactory();

        Map<String, WxMpService> services = serviceFactory.createServices(appCredentialGroup, null);

        WxMpConfigStorage configStorage = services.get("wx-mp-a").getWxMpConfigStorage();
        assertEquals(1, services.size());
        assertInstanceOf(WxMpDefaultConfigImpl.class, configStorage);
        assertEquals("wx-mp-a", configStorage.getAppId());
        assertEquals("mp-secret-a", configStorage.getSecret());
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
                WechatServiceFactoryTest.class.getClassLoader(),
                new Class<?>[]{RedisConnectionFactory.class},
                (proxy, method, args) -> null
        );
    }
}
