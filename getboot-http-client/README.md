# getboot-http-client

HTTP 客户端出站请求头增强 starter。

## 作用

- 提供 OpenFeign 出站 `traceId` 自动透传能力
- 提供 WebClient 出站 `traceId` 自动透传能力
- 提供 RestTemplate 出站 `traceId` 自动透传能力
- 提供模块级通用出站请求头贡献 SPI，便于统一补充租户、应用标识等请求头
- 保留客户端专属扩展点，便于继续补充 Feign / WebClient / RestTemplate 特定定制

## 接入方式

业务项目继承父 `pom` 后，按需引入：

```xml
<dependency>
    <groupId>com.dt</groupId>
    <artifactId>getboot-http-client</artifactId>
</dependency>
```

适合这几类场景：

- 业务项目已经在用 OpenFeign、WebClient 或 RestTemplate
- 想统一补 `traceId`、租户标识、应用标识等出站请求头
- 想把 HTTP 客户端透传逻辑从业务代码里拿掉

## 前置条件

- 项目里至少要实际使用一种 HTTP 客户端能力，模块增强才有落点
- 如果你想获得完整链路 Trace 体验，通常会和 `getboot-observability` 一起使用

## 目录约定

- `api.properties`：三类 HTTP 客户端的 Trace 配置模型
- `api.model`：通用出站请求上下文模型
- `spi`：模块级通用出站请求头贡献 SPI
- `spi.feign`：OpenFeign 请求定制扩展点
- `spi.webclient`：WebClient 请求定制扩展点
- `spi.resttemplate`：RestTemplate 请求定制扩展点
- `support.headers`：公共出站请求头解析能力
- `infrastructure.headers.*`：公共出站请求头自动配置
- `infrastructure.feign.*`：OpenFeign 自动装配与实现
- `infrastructure.webclient.*`：WebClient 自动装配与实现
- `infrastructure.resttemplate.*`：RestTemplate 自动装配与实现

## 配置示例

```yaml
getboot:
  http-client:
    openfeign:
      trace:
        enabled: true                     # 是否启用 OpenFeign TraceId 透传
        header-name: X-Trace-Id           # OpenFeign 出站请求头名称
    webclient:
      trace:
        enabled: true                     # 是否启用 WebClient TraceId 透传
        header-name: X-Trace-Id           # WebClient 出站请求头名称
    resttemplate:
      trace:
        enabled: true                     # 是否启用 RestTemplate TraceId 透传
        header-name: X-Trace-Id           # RestTemplate 出站请求头名称
```

## 默认 Bean

- `RequestInterceptor`：Bean 名称为 `getbootTraceFeignRequestInterceptor`
- `ExchangeFilterFunction`：Bean 名称为 `getbootTraceWebClientFilterFunction`
- `WebClientCustomizer`：Bean 名称为 `getbootTraceWebClientCustomizer`
- `ClientHttpRequestInterceptor`：Bean 名称为 `getbootTraceRestTemplateInterceptor`
- `RestTemplateCustomizer`：Bean 名称为 `getbootTraceRestTemplateCustomizer`
- `OutboundHttpHeadersResolver`：Bean 名称为 `getbootOutboundHttpHeadersResolver`

## 扩展点

- `OutboundHttpHeadersContributor`
- `OpenFeignTraceRequestCustomizer`
- `WebClientTraceRequestCustomizer`
- `RestTemplateTraceRequestCustomizer`
- `OutboundHttpHeadersContributor` 适合客户端无关的通用请求头增强，例如租户标识、应用标识、语言环境
- 技术栈专属 `*TraceRequestCustomizer` 仍然保留，适合依赖 Feign / WebClient / RestTemplate 原生请求模型的定制
- 即便未引入 `getboot-observability`，HTTP 客户端透传能力也可独立工作；只是没有统一链路上下文时，会退回到请求头透传逻辑
- 当前模块只处理出站 HTTP 客户端能力，不承接服务端 Web 能力，也不承接 Dubbo、gRPC 等 RPC 技术栈

## 已实现技术栈

- 通用出站头增强
- OpenFeign
- WebClient
- RestTemplate

## 边界 / 补充文档

- 业务项目统一使用 `getboot.http-client.*`
- OpenFeign 配置使用 `getboot.http-client.openfeign.trace.*`
- WebClient 配置使用 `getboot.http-client.webclient.trace.*`
- RestTemplate 配置使用 `getboot.http-client.resttemplate.trace.*`
- 当前模块只承接出站 HTTP 横切增强，不提供统一 HTTP 调用门面
- 如果你要补客户端无关的公共请求头，优先实现 `OutboundHttpHeadersContributor`
- 如果你要补依赖具体客户端原生请求模型的逻辑，再实现对应技术栈的 `*TraceRequestCustomizer`
