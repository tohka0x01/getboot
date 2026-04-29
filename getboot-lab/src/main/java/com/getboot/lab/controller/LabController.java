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
package com.getboot.lab.controller;

import com.getboot.exception.api.code.CommonErrorCode;
import com.getboot.exception.api.exception.BusinessException;
import com.getboot.lab.api.CapabilityItem;
import com.getboot.lab.api.CapabilityOverview;
import com.getboot.lab.api.EchoRequest;
import com.getboot.lab.api.EchoResult;
import com.getboot.lab.api.TraceCheckResult;
import com.getboot.support.api.trace.TraceContextHolder;
import com.getboot.web.api.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 能力验证接口
 *
 * @author qiheng
 */
@RestController
@RequestMapping("/api/lab")
public class LabController {

    /**
     * 日志对象
     */
    private static final Logger log = LoggerFactory.getLogger(LabController.class);

    /**
     * 当前应用名
     */
    private final String applicationName;

    /**
     * Spring 环境
     */
    private final Environment environment;

    /**
     * 创建验证接口
     *
     * @param applicationName 当前应用名
     * @param environment Spring 环境
     */
    public LabController(
            @Value("${spring.application.name:getboot-lab}") String applicationName,
            Environment environment) {
        this.applicationName = applicationName;
        this.environment = environment;
    }

    /**
     * 查询能力验证总览
     *
     * @return 能力验证总览
     */
    @GetMapping("/overview")
    public ApiResponse<CapabilityOverview> overview() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return ApiResponse.success(new CapabilityOverview(
                applicationName,
                profiles.isEmpty() ? List.of("default") : profiles,
                System.getProperty("java.version"),
                capabilities()
        ));
    }

    /**
     * 验证统一响应和 Trace 写入
     *
     * @param request 当前请求
     * @return Trace 验证结果
     */
    @GetMapping("/checks/trace")
    public ApiResponse<TraceCheckResult> trace(HttpServletRequest request) {
        String traceId = TraceContextHolder.getTraceId();
        log.info("GetBoot trace check passed, traceId={}", traceId);
        return ApiResponse.success(new TraceCheckResult(
                request.getHeader("X-Trace-Id"),
                request.getAttribute("GETBOOT_TRACE_ID"),
                traceId,
                MDC.get("traceId")
        ));
    }

    /**
     * 验证统一响应对象
     *
     * @return 成功响应
     */
    @GetMapping("/checks/web-success")
    public ApiResponse<Map<String, Object>> webSuccess() {
        return ApiResponse.success(Map.of(
                "module", "getboot-web",
                "check", "统一响应",
                "passed", true
        ));
    }

    /**
     * 验证请求体回显
     *
     * @param request Echo 请求
     * @return Echo 结果
     */
    @PostMapping("/checks/echo")
    public ApiResponse<EchoResult> echo(@RequestBody EchoRequest request) {
        return ApiResponse.success(new EchoResult(
                request.content(),
                request.attributes(),
                TraceContextHolder.getTraceId()
        ));
    }

    /**
     * 验证业务异常收口
     */
    @PostMapping("/checks/business-exception")
    public void businessException() {
        throw new BusinessException(CommonErrorCode.PARAM_ERROR);
    }

    /**
     * 组装能力清单
     *
     * @return 能力清单
     */
    private List<CapabilityItem> capabilities() {
        return List.of(
                ready("getboot-support", "通用支撑", "基础能力", "Trace 上下文、环境配置别名和公共支撑对象", "/api/lab/checks/trace"),
                ready("getboot-exception", "统一异常", "基础能力", "错误码、业务异常和全局异常收口", "/api/lab/checks/business-exception"),
                ready("getboot-web", "Web 响应", "基础能力", "统一响应模型、异常处理和参数错误返回", "/api/lab/checks/web-success"),
                ready("getboot-observability", "可观测性", "基础能力", "Trace 解析、短 tid、响应头和日志 MDC", "/api/lab/checks/trace"),
                external("getboot-cache", "Redis 缓存", "基础设施", "RedisTemplate、缓存门面和序列化策略", "Redis"),
                external("getboot-coordination", "协调底座", "基础设施", "Redisson、Curator 和协调基础设施", "Redis / ZooKeeper"),
                external("getboot-database", "数据访问", "基础设施", "数据源、MyBatis-Plus、MongoDB 和 ShardingSphere", "MySQL / MongoDB"),
                external("getboot-storage", "对象存储", "基础设施", "上传、下载、删除和预签名 URL", "MinIO / OSS"),
                external("getboot-sms", "短信发送", "基础设施", "模板短信、验证码短信和供应商适配", "短信供应商账号"),
                external("getboot-mail", "邮件发送", "基础设施", "SMTP、模板变量和附件发送", "SMTP 账号"),
                external("getboot-search", "搜索能力", "基础设施", "索引写入、基础查询、分页和高亮", "Elasticsearch / OpenSearch"),
                external("getboot-auth", "认证能力", "横切能力", "Sa-Token 登录态、当前用户和权限校验", "Redis 可选"),
                external("getboot-limiter", "分布式限流", "横切能力", "滑动窗口、令牌桶和漏桶限流", "Redis / Redisson"),
                external("getboot-lock", "分布式锁", "横切能力", "注解式锁、锁键解析和多后端实现", "Redis / ZooKeeper / Database"),
                external("getboot-idempotency", "幂等去重", "横切能力", "幂等键、重复请求复用和 TTL 管理", "Redis"),
                external("getboot-governance", "流量治理", "横切能力", "Sentinel 规则、熔断和流控接入", "Sentinel"),
                external("getboot-transaction", "分布式事务", "横切能力", "Seata 配置和事务边界保护", "Seata"),
                external("getboot-webhook", "Webhook", "横切能力", "回调验签、限流和幂等编排", "Redis 可选"),
                external("getboot-http-client", "HTTP 客户端", "通信能力", "Feign、WebClient、RestTemplate 出站透传", "目标 HTTP 服务"),
                external("getboot-rpc", "Dubbo RPC", "通信能力", "Dubbo Trace 透传、认证和序列化安全", "Dubbo / Nacos"),
                external("getboot-mq", "消息队列", "通信能力", "RocketMQ、Kafka、MQTT 生产和 Trace 透传", "RocketMQ / Kafka / MQTT"),
                external("getboot-websocket", "WebSocket", "通信能力", "会话注册、在线状态和定向推送", "无"),
                external("getboot-job", "任务调度", "通信能力", "XXL-JOB 执行器和客户端", "XXL-JOB Admin"),
                external("getboot-wechat", "微信生态", "生态能力", "公众号、小程序和微信开放平台", "微信应用配置"),
                external("getboot-payment", "支付能力", "生态能力", "支付宝、微信支付和回调验签", "支付商户配置")
        );
    }

    /**
     * 创建可直接验证的能力项
     *
     * @param module 模块名
     * @param name 能力名称
     * @param category 能力分组
     * @param description 能力说明
     * @param actionPath 验证接口
     * @return 能力项
     */
    private CapabilityItem ready(
            String module,
            String name,
            String category,
            String description,
            String actionPath) {
        return new CapabilityItem(module, name, category, "可手测", description, actionPath, "内置");
    }

    /**
     * 创建依赖外部环境的能力项
     *
     * @param module 模块名
     * @param name 能力名称
     * @param category 能力分组
     * @param description 能力说明
     * @param dependency 依赖环境
     * @return 能力项
     */
    private CapabilityItem external(
            String module,
            String name,
            String category,
            String description,
            String dependency) {
        return new CapabilityItem(module, name, category, "待接环境", description, null, dependency);
    }
}
