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
package com.getboot.auth.infrastructure.satoken.autoconfigure;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.stp.StpUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sa-Token Servlet 认证过滤集成测试。
 *
 * @author qiheng
 */
@SpringBootTest(
        classes = SaTokenServletFilterIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "getboot.auth.satoken.token-name=Authorization",
                "getboot.auth.satoken.is-log=false",
                "getboot.auth.satoken.servlet.filter.enabled=true",
                "getboot.auth.satoken.servlet.filter.include-paths[0]=/secure/**"
        }
)
class SaTokenServletFilterIntegrationTest {

    /**
     * 测试客户端。
     */
    @Autowired
    private TestRestTemplate testRestTemplate;

    /**
     * 验证未登录访问受保护接口时会返回 401。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldRejectProtectedPathWhenNotLoggedIn() {
        ResponseEntity<Map> response = testRestTemplate.getForEntity("/secure/ping", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody())
                .containsEntry("status", "fail")
                .containsEntry("code", 401)
                .containsEntry("message", "Unauthorized");
    }

    /**
     * 验证完成登录后可访问受保护接口。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldAllowProtectedPathAfterLogin() {
        ResponseEntity<Map> loginResponse = testRestTemplate.getForEntity("/login", Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        String token = String.valueOf(loginResponse.getBody().get("token"));
        assertThat(token).isNotBlank();

        RequestEntity<Void> request = RequestEntity.get(URI.create("/secure/ping"))
                .header("Authorization", token)
                .build();

        ResponseEntity<Map> response = testRestTemplate.exchange(request, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("status", "ok")
                .containsEntry("loginId", "1001");
    }

    /**
     * 验证 OPTIONS 预检请求不会被认证过滤器提前拒绝。
     */
    @Test
    void shouldSkipOptionsPreflightRequest() {
        RequestEntity<Void> request = new RequestEntity<>(null, HttpMethod.OPTIONS, URI.create("/secure/ping"));

        ResponseEntity<Void> response = testRestTemplate.exchange(request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * 测试用应用。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @RestController
    static class TestApplication {

        /**
         * 使用内存版 Sa-Token Dao，避免测试依赖外部 Redis。
         *
         * @return 内存版 Dao
         */
        @Bean
        @Primary
        public SaTokenDao saTokenDao() {
            return new SaTokenDaoDefaultImpl();
        }

        /**
         * 执行测试登录并返回 token。
         *
         * @return 登录结果
         */
        @GetMapping("/login")
        public Map<String, String> login() {
            StpUtil.login(1001L);
            return Map.of("token", StpUtil.getTokenValue());
        }

        /**
         * 受保护的测试接口。
         *
         * @return 返回结果
         */
        @RequestMapping(path = "/secure/ping", method = RequestMethod.GET)
        public Map<String, String> securePing() {
            return Map.of("status", "ok", "loginId", String.valueOf(StpUtil.getLoginId()));
        }

        /**
         * 为预检请求提供独立响应，验证过滤器跳过后不会再触发登录校验。
         */
        @RequestMapping(path = "/secure/ping", method = RequestMethod.OPTIONS)
        @ResponseStatus(HttpStatus.OK)
        public void securePingOptions() {
        }
    }
}
