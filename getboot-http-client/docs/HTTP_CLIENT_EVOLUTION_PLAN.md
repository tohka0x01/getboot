# getboot-http-client 演进规划

这份文档对应 `docs/TODO.md` 里这项工作：

- 在 `getboot-http-client` 继续评估是否补充更通用的客户端封装或新的实现子树

它不是当前接入文档。当前 `getboot-http-client` 怎么引、怎么配、现在已经提供什么能力，先看 [`../README.md`](../README.md)；这份文档只回答“这个模块后续应该怎么扩，而不是怎么变成另一个 HTTP SDK”。

## 1. 当前状态

当前 `getboot-http-client` 已经明确提供的能力只有一类：

- 出站 HTTP 请求的 `traceId` 透传

当前已实现子树：

- `infrastructure.feign.*`
- `infrastructure.webclient.*`
- `infrastructure.resttemplate.*`

当前稳定暴露给业务方的扩展点：

- `OpenFeignTraceRequestCustomizer`
- `WebClientTraceRequestCustomizer`
- `RestTemplateTraceRequestCustomizer`

当前稳定配置入口：

- `getboot.http-client.openfeign.trace.*`
- `getboot.http-client.webclient.trace.*`
- `getboot.http-client.resttemplate.trace.*`

因此，这个模块今天的真实定位不是“统一 HTTP 客户端框架”，而是：

- HTTP 出站横切增强模块

## 2. 当前问题到底是什么

`docs/TODO.md` 里这项任务，真正要回答的是两个问题：

1. 要不要继续做一个更通用的客户端封装？
2. 要不要继续加新的实现子树？

这两个问题不能混在一起处理。

因为：

- “更通用的客户端封装”是在改能力边界
- “新的实现子树”是在扩实现覆盖面

两者的风险和收益完全不同。

## 3. 评估结论

当前结论是：

- 不建议现在就把 `getboot-http-client` 升级成通用 HTTP 客户端门面
- 建议继续保持“出站 HTTP 横切增强模块”定位
- 如果后续新增实现，优先走新的 `infrastructure.<client>` 子树
- 通用能力如果要补，优先补“共性出站头透传 / 上下文传播”层，而不是统一请求发送 API

换句话说：

- 不要急着做 `HttpClientOperator`
- 也不要急着统一所有 HTTP 客户端的发送方法
- 先把横切增强的能力层收紧，再决定是否值得继续抽更上层 API

## 4. 为什么不建议立刻做通用客户端封装

当前模块服务的三类客户端：

- OpenFeign
- WebClient
- RestTemplate

它们在使用方式上并不等价：

- OpenFeign 是声明式接口代理
- WebClient 是响应式 fluent API
- RestTemplate 是同步模板调用

如果现在为了“看起来统一”而强行做一个新的通用客户端门面，通常会遇到这些问题：

- 把不同调用模型压成最低公分母，语义变弱
- 业务侧反而需要在门面外再回到原生客户端
- 为了兼容同步 / 异步 / 响应式，又会把能力层越做越大
- 最后模块维护的不是“透传增强”，而是“另一个半成品 HTTP SDK”

这和仓库当前的原则相冲突：

- 保持能力层稳定
- 不为了统一而制造更重的抽象

## 5. 当前真正值得统一的部分

虽然不建议直接做通用发送门面，但当前模块里确实有一层值得继续统一：

- 出站头透传
- Trace 上下文读取
- 统一 header 名称处理
- 统一业务扩展点调用时机

这意味着后续更合理的方向是：

- 保持不同客户端各自的原生调用方式
- 在模块内部抽出公共“出站头增强”逻辑
- 让不同实现子树都复用这层公共逻辑

一句话概括：

- 统一横切行为
- 不统一发送编程模型

## 6. 当前模块里已经重复的东西

现在三类实现里已经存在明显重复：

- 都要从 `TraceContextHolder` 读取 `traceId`
- 都要判断是否启用透传
- 都要写入 header-name
- 都要在透传后再调用各自 customizer

这些重复值得继续收敛，但收敛位置应该优先放在：

- `support`
- 或一个新的通用 `spi`

而不是直接变成业务侧新的统一客户端。

## 7. 推荐的演进方向

### 7.1 第一优先级：继续保持模块定位

模块对外定位继续保持：

- HTTP 出站横切增强

不改成：

- 统一 HTTP 调用框架

### 7.2 第二优先级：抽共性增强层

下一阶段如果继续演进，建议优先抽这类共性：

- 统一 TraceId 解析逻辑
- 统一 header 注入逻辑
- 统一空值 / 开关判断逻辑
- 统一 customizer 调用链顺序

这类抽象可以是：

- 内部 `support`
- 或一个模块通用 SPI

但前提是不要把底层客户端类型完全抹掉。

### 7.3 第三优先级：再考虑通用扩展点

如果后续确实发现三类客户端都需要同一种“与客户端无关的出站头增强”，可以再考虑新增模块通用 SPI，例如：

- 通用出站头贡献器
- 通用上下文头提供器

这种 SPI 的价值在于：

- 它描述“要加哪些头”
- 而不是“怎么发请求”

这类扩展点比统一客户端门面更符合当前模块定位。

## 8. 新实现子树的判断标准

后续可以新增实现子树，但需要满足两个条件：

1. 该客户端在业务项目里足够常见
2. 它的横切增强逻辑和现有模块目标一致

可能的候选方向：

- `infrastructure.okhttp.*`
- `infrastructure.apachehc5.*`
- `infrastructure.jdkhttp.*`

不建议因为某个非常边缘或一次性的客户端就新增实现子树。

判断标准应是：

- 它是否真的是“团队反复会用到的出站 HTTP 客户端”

而不是：

- 它恰好也能发 HTTP 请求

## 9. 配置规划

当前配置继续保持：

- `getboot.http-client.openfeign.trace.*`
- `getboot.http-client.webclient.trace.*`
- `getboot.http-client.resttemplate.trace.*`

后续如果新增新的实现子树，建议继续沿用同样模式：

```yaml
getboot:
  http-client:
    okhttp:
      trace:
        enabled: true
        header-name: X-Trace-Id
```

规划原则：

- 配置前缀继续表达“能力 + 客户端实现”
- 不回退到原生客户端前缀作为主要业务入口
- 新实现沿用同一套 `trace` 子树结构

当前不建议直接引入新的全局通用前缀，例如：

- `getboot.http-client.trace.*`

除非未来至少两类以上实现已经明确需要“全局默认值 + 局部覆盖”这种能力；否则会让配置变复杂，但收益有限。

## 10. SPI 规划

当前模块已经有三类技术栈专属 SPI：

- `spi.feign`
- `spi.webclient`
- `spi.resttemplate`

这套结构目前是合理的，因为它们直接暴露了各自客户端原生类型。

后续如果继续演进，建议遵循下面的顺序：

1. 先保留现有技术栈专属 SPI
2. 只有当“与具体客户端无关的出站头增强”场景足够明确时，才新增模块通用 SPI
3. 不要为了“结构完整”而凭空造一个大而泛的 `spi`

也就是说：

- 先有真实共性需求
- 再抽公共 SPI

## 11. 和其他模块的边界

`getboot-http-client` 应继续只管：

- 出站 HTTP 客户端横切增强

不应越界到：

- 服务端 Web 过滤器
- Dubbo / gRPC / MQ 等非 HTTP 通信能力
- 重型重试、熔断、限流治理总控
- 统一第三方 API SDK 门面

这些边界分别更适合留在：

- `getboot-web`
- `getboot-rpc`
- `getboot-governance`
- 各业务生态模块

## 12. 推荐的落地顺序

建议这样推进：

1. 先补这份方向文档，明确模块不转向通用 HTTP SDK
2. 先在模块内部抽共性 Trace / header 增强逻辑
3. 再评估是否需要通用出站头 SPI
4. 只有在真实使用场景出现后，再新增新的 `infrastructure.<client>` 子树
5. 最后才决定是否需要更强的通用配置层

原因：

- 先稳住模块边界，能避免一上来把三种客户端调用模型揉坏
- 先做内部共性抽取，能在不破坏业务接入的情况下收敛重复实现
- 新实现后补，才能保证“因为真实需求而扩实现”，而不是“因为设计想象而扩实现”

## 13. 完成标准

这项 TODO 可以认为真正完成，至少满足：

- 已经明确 `getboot-http-client` 仍然定位为“出站 HTTP 横切增强模块”
- 已经写清楚“更通用的客户端封装”当前为什么不应优先做
- 如果后续新增实现，目录应继续落在 `infrastructure.<client>.*`
- 文档已经区分“共性横切增强”和“统一发送门面”是两回事
- README 能明确说明当前模块只承接出站增强，不承接通用 HTTP SDK 封装
