# getboot-rpc

RPC 远程调用增强 starter，当前提供 Dubbo 场景下的请求签名校验、调用方凭证解析、Trace 透传与序列化安全配置。

## 作用

- 提供 Dubbo Trace 透传与认证增强
- 提供调用方凭证解析和请求签名校验
- 提供 Dubbo 序列化安全配置初始化

## 接入方式

业务项目继承父 `pom` 后，按需引入：

```xml
<dependency>
    <groupId>com.dt</groupId>
    <artifactId>getboot-rpc</artifactId>
</dependency>
```

适合这几类场景：

- 想把 Dubbo 的鉴权、附件透传和序列化安全统一收口
- 想对消费方 / 提供方凭证管理留出可替换的稳定扩展点
- 后续可能新增其他 RPC 实现，但希望对外仍沿用统一远程调用模块边界

## 前置条件

- 需要准备可用的 Dubbo 运行环境与注册中心配置
- 若启用提供方鉴权，需要提供合法的调用方凭证配置，或自行实现 `RpcCallerSecretResolver`
- `getboot-observability` 不是前置模块；Dubbo Trace 透传能力可以独立工作
- 如果要自定义签名算法、认证附件或额外校验逻辑，应提前规划对应 SPI Bean

## 目录约定

- `api.properties`：对外稳定配置模型
- `api.resolver`：调用方密钥解析接口
- `spi`：能力层签名扩展点
- `spi.dubbo`：Dubbo 专属扩展点
- `support.authentication`：默认认证实现
- `infrastructure.dubbo.*`：Dubbo 实现与自动装配

## 配置示例

```yaml
getboot:
  rpc:
    trace:
      enabled: true               # 是否启用 Dubbo TraceId 透传
      mdc-key: traceId            # 日志 MDC 中使用的键名
    dubbo:
      application:
        name: demo-service        # Dubbo 应用名称
    security:
      authentication:
        enabled: true               # 是否启用 RPC 鉴权
        allowed-clock-skew-seconds: 300 # 时间戳允许偏差，单位秒
        excluded-service-prefixes:
          - org.apache.dubbo.       # 不参与鉴权的服务前缀
        consumer:
          app-id: consumer-app      # 消费方应用标识
          app-secret: consumer-secret # 消费方签名密钥
        provider:
          required: true            # 提供方是否强制要求鉴权
          credentials:
            consumer-app: consumer-secret # 提供方认可的调用方凭证
      serialization:
        enabled: true               # 是否启用 Dubbo 序列化安全控制
        check-status: STRICT        # 校验级别
        check-serializable: true    # 是否要求对象实现 Serializable
        allowed-prefixes:
          - com.getboot             # 允许反序列化的包前缀
```

## 默认 Bean

- `RpcAuthenticationSigner`：默认实现为 `DefaultRpcAuthenticationSigner`
- `RpcCallerSecretResolver`：默认实现为 `PropertiesRpcCallerSecretResolver`
- `RpcSecurityConfigurationValidator`：RPC 安全配置校验器
- `RpcSerializationSecurityInitializer`：Dubbo 序列化安全初始化器

## 扩展点

- 业务项目统一使用 `getboot.rpc.*`
- 安全配置统一使用 `getboot.rpc.security.*`
- Trace 配置统一使用 `getboot.rpc.trace.*`
- Dubbo 原生配置统一使用 `getboot.rpc.dubbo.*`
- RPC 安全与 Trace 配置模型统一收敛到 `com.getboot.rpc.api.properties.*`
- 调用方密钥解析接口统一收敛到 `com.getboot.rpc.api.resolver.*`
- RPC 能力层签名扩展点统一收敛到 `com.getboot.rpc.spi.*`
- Dubbo 认证扩展点统一收敛到 `com.getboot.rpc.spi.dubbo.*`
- 可通过注册 `RpcAuthenticationSigner` Bean 自定义签名算法
- 可通过注册 `RpcAuthenticationAttachmentCustomizer` Bean 增加消费端认证附件
- 可通过注册 `RpcAuthenticationValidationHook` Bean 追加提供端认证校验逻辑
- 可通过注册 `RpcCallerSecretResolver` Bean 替换默认调用方密钥解析方式

## 已实现技术栈

- Dubbo

## 边界 / 补充文档

- 当前模块主要负责 Dubbo 场景下的 Trace、鉴权与序列化安全增强，不试图抽象统一 RPC 服务定义模型
- `getboot.rpc.dubbo.*` 会桥接到底层 `dubbo.*`；`getboot.rpc.trace.*` 与 `getboot.rpc.security.*` 由模块自己的 `@ConfigurationProperties` 承接
- 当前线程存在 `traceId` 时，会自动通过 Dubbo attachment 向下游透传；提供端收到后会自动回填到 `TraceContextHolder` 与日志 `MDC`
- 当前内部实现只有 Dubbo；后续如果新增 gRPC，也仍沿用 `getboot-rpc` 作为统一远程调用模块
- 可直接参考 `src/main/resources/getboot-rpc.yml.example`
