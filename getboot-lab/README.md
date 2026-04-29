# getboot-lab

GetBoot 能力验证台，用来把公共 starter 的能力变成可以手动验证、可以截图、可以复现的问题排查入口。

它不是业务 starter，也不建议被业务系统依赖。它的定位是：

- 本地开发时验证公共能力是否正常
- 发版前跑一遍基础能力验收
- 新人接入 GetBoot 时快速看到每个模块解决什么问题
- 后续逐步补齐 Redis、MQ、Dubbo、支付、短信、对象存储等真实环境验证

## 启动

```bash
mvn -pl getboot-lab -am spring-boot:run
```

默认地址：

```text
http://127.0.0.1:18080/
```

## 第一版已覆盖

- `getboot-web`：统一成功响应、业务异常统一返回
- `getboot-observability`：请求头 TraceId、响应头 TraceId、`meta.traceId`、日志 MDC
- `getboot-support`：`TraceContextHolder` 当前线程上下文
- `getboot-exception`：`BusinessException` 和 `CommonErrorCode`

## 后续补齐顺序

1. Redis：`getboot-cache`、`getboot-lock`、`getboot-limiter`、`getboot-idempotency`
2. HTTP 通信：`getboot-http-client` 的 RestTemplate、WebClient、Feign 透传
3. 数据能力：`getboot-database`、`getboot-transaction`
4. 消息与 RPC：`getboot-mq`、`getboot-rpc`
5. 生态能力：`getboot-storage`、`getboot-sms`、`getboot-mail`、`getboot-wechat`、`getboot-payment`

外部依赖类能力不要做假按钮，必须能真实连接环境、真实执行请求、真实展示结果。
