# getboot-websocket

统一 WebSocket starter，当前基于 `jakarta.websocket` 承接会话注册、按用户/会话推送和文本消息监听。

## 作用

- 提供统一会话注册表 `WebSocketSessionRegistry`
- 提供统一推送门面 `WebSocketMessageSender`
- 自动注册默认 WebSocket Endpoint
- 提供连接生命周期与文本消息监听扩展点

## 接入方式

业务项目继承父 `pom` 后，按需引入：

```xml
<dependency>
    <groupId>com.dt</groupId>
    <artifactId>getboot-websocket</artifactId>
</dependency>
```

适合这几类场景：

- 业务服务需要 WebSocket 长连接推送，但不想在项目里重复维护会话表
- 你想统一“按用户推送 / 按会话推送 / 广播”这类基础能力入口
- 你想把连接建立、断开和文本消息监听通过 Spring Bean 扩展，而不是散落在业务配置里

## 前置条件

- 当前实现基于 `jakarta.websocket`
- 当前默认落在 Servlet 容器场景
- 如果业务项目需要 STOMP 协议编排或消息代理，不属于这个模块第一阶段边界

## 目录约定

- `api.properties`：WebSocket 配置模型
- `api.registry`：会话注册表接口
- `api.sender`：统一推送门面
- `spi`：用户标识解析、连接生命周期和文本消息监听扩展点
- `support`：默认注册表、默认推送器和默认用户标识解析器
- `infrastructure.jakarta.*`：Endpoint、握手配置器和自动装配

## 配置示例

```yaml
getboot:
  websocket:
    enabled: true
    endpoint: /getboot/ws
    allowed-origins:
      - https://demo.example.com
    trace-header-name: X-Trace-Id
    user-id-header-name: X-User-Id
    user-id-query-parameter: userId
    async-send-timeout-ms: 10000
```

## 默认 Bean

- `WebSocketSessionRegistry`
- `WebSocketMessageSender`
- `WebSocketUserIdResolver`
- `GetbootWebSocketEndpoint`
- `ServletContextInitializer`

## 扩展点

- `WebSocketUserIdResolver`
- `WebSocketSessionLifecycleListener`
- `WebSocketTextMessageListener`
- 如果业务方需要替换默认会话注册表或推送门面，可直接覆盖 `WebSocketSessionRegistry` / `WebSocketMessageSender`

## 已实现技术栈

- `jakarta.websocket`
- Servlet WebSocket Container

## 边界 / 补充说明

- 当前模块只负责基础长连接收口，不承接 STOMP、消息代理、订阅授权编排
- 当前默认只处理文本消息；二进制消息和复杂协议协商不在第一阶段边界
- `sendToUser(...)` 依赖连接时解析出的用户标识；默认从请求头和查询参数读取，业务可自行覆盖
