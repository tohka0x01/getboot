# getboot-database MongoDB 方向规划

这份文档对应 `docs/TODO.md` 里这项工作：

- 在 `getboot-database` 评估并预留 `MongoDB` 实现方向

它不是当前接入文档。当前 `getboot-database` 怎么引、怎么配、现在已经提供什么能力，先看 [`../README.md`](../README.md)；这份文档只回答“MongoDB 如果进入这个模块，边界该怎么划”。

## 1. 当前状态

当前 `getboot-database` 明确已经提供的能力是：

- 数据源预热与连通性校验
- MyBatis-Plus 拦截器
- 审计字段自动填充
- ShardingSphere 规则文件与数据源装配

当前实现树完全围绕关系型数据访问展开：

- `infrastructure.datasource.*`
- `infrastructure.mybatisplus.*`
- `infrastructure.sharding.*`

当前配置模型也是关系型优先：

- `getboot.database.datasource.*`
- `getboot.database.mybatis-plus.*`
- `getboot.database.sharding.*`

因此，MongoDB 方向不是“顺手再补一个 starter”，而是一次明确的模块边界判断。

## 2. 评估结论

当前结论是：

- `getboot-database` 仍然可以承接 `MongoDB`
- 但前提是把它定位成“数据访问基础设施的另一条实现子树”
- 不能把 MongoDB 伪装成 `DataSource` / MyBatis / Sharding 的同义扩展

也就是说，MongoDB 如果进入 `getboot-database`，成立的原因只能是：

- 它仍然属于“业务服务的数据访问底座能力”

而不是：

- 它看起来也算数据库，所以什么都塞进现有关系型模型里

## 3. 不应该被推翻的现有边界

MongoDB 方向进入后，下面这些边界应继续保持稳定：

- `getboot.database.datasource.*` 仍然只表达关系型数据源语义
- `getboot.database.mybatis-plus.*` 仍然只表达 MyBatis-Plus 语义
- `getboot.database.sharding.*` 仍然只表达 ShardingSphere 语义
- `DataSourceInitializer` 不应被扩展成“所有数据库统一初始化器”
- `MybatisPlusInterceptor`、`AuditFieldMetaObjectHandler` 不应为了兼容 MongoDB 被抬成伪通用接口

一句话说清楚：

- MongoDB 可以进入模块
- 但不能污染已经稳定的关系型子树

## 4. MongoDB 进入后的定位

MongoDB 如果要进入 `getboot-database`，建议只承接这些事情：

- 统一 `MongoClient` / `MongoTemplate` 接入
- 统一配置前缀与环境桥接
- 统一 Mapping / Converter / 序列化相关默认装配
- 统一索引初始化、启动校验、可选事务会话等基础能力
- 暴露必要的 Mongo 专属扩展点

不建议在这里承接的事情：

- 全文检索能力
- 向量检索能力
- 文档搜索 DSL 的统一抽象
- 业务级仓储规范
- 把 MongoDB 生硬塞进 `DataSource` 语义

这些能力如果后续继续膨胀，更可能属于：

- `getboot-search`
- 独立文档数据库模块
- 业务仓储层自己建模

## 5. 推荐的目录形态

MongoDB 方向建议使用单独实现子树：

```text
com.getboot.database
├── api
│   └── properties
├── spi
├── support
└── infrastructure
    ├── datasource
    ├── mybatisplus
    ├── sharding
    └── mongodb
        ├── autoconfigure
        ├── environment
        ├── properties
        └── support
```

这样做的好处：

- 关系型和文档型实现边界一眼可见
- 现有 `datasource` / `mybatisplus` / `sharding` 不需要改语义
- 后续如果 Mongo 方向停止在基础接入层，仍然是合理的同模块实现

## 6. 配置规划

当前关系型配置继续保持不变。MongoDB 如果进入，建议新增独立子树：

```yaml
getboot:
  database:
    enabled: true
    mongodb:
      enabled: false
      uri: mongodb://127.0.0.1:27017/demo
      database: demo
      auto-index-creation: false
      init:
        enabled: true
        timeout: 5000
        strict-mode: false
```

规划原则：

- MongoDB 不复用 `datasource.*`
- MongoDB 不混入 `sharding.*`
- MongoDB 自己的启动校验放在 `mongodb.init.*`
- 如果需要桥接到底层原生前缀，优先映射到 `spring.data.mongodb.*`

换句话说，后续如果做配置桥接，应该是：

- `getboot.database.mongodb.*` -> `spring.data.mongodb.*`

而不是：

- 让业务项目直接回到原生 `spring.data.mongodb.*` 作为主入口

## 7. 默认能力的判断标准

MongoDB 进入模块后，默认只应承接“大家都会反复配、而且值得统一”的底层能力。

适合做默认能力的方向：

- `MongoClient` / `MongoTemplate` 自动装配
- 常见 converter / object mapper 定制入口
- 启动阶段连接检查
- 可选索引初始化
- 可选审计回调

不适合一开始就做统一能力的方向：

- 复杂聚合 DSL
- 仓储模板大而全封装
- 分片路由统一抽象
- Mongo 与关系型混合事务统一抽象

这些能力如果一开始就做，会很容易把模块从“基础设施接入层”拉成“通用持久化框架”。

## 8. SPI 规划

MongoDB 如果进入，建议优先暴露少量、稳定、明显属于基础接入层的扩展点。

可以考虑的方向：

- `MongoClientSettings` 定制
- `MongoTemplate` 定制
- Mapping / Converter 定制
- 索引初始化策略定制

但在没有明确需求前，不建议先发明一组新的伪通用 SPI。

推荐原则：

- 真正跨 Mongo 接入场景都需要扩展的，再放 `spi.mongodb`
- 只是实现内部协作的，继续留在 `infrastructure.mongodb.*`

## 9. 和现有关系型能力的关系

MongoDB 进入模块后，应该和现有关系型子树并列，而不是上下级关系。

关系如下：

- `datasource`
  关系型连接与启动校验
- `mybatisplus`
  关系型 ORM 增强
- `sharding`
  关系型分库分表
- `mongodb`
  文档数据库接入增强

这意味着：

- MongoDB 不依赖 MyBatis-Plus
- MongoDB 不依赖 ShardingSphere
- 关系型数据源预热逻辑不自动套到 MongoDB

如果未来真的需要“关系型 + Mongo”的统一上层抽象，也不应直接在当前模块里把它们捏成同一套持久化 API。

## 10. 与其他规划模块的边界

MongoDB 进入 `getboot-database` 只在下面这个边界内成立：

- 把 Mongo 当作业务服务常见的数据访问底座之一

如果诉求变成下面这些，应该重新评估模块归属：

- 把 Mongo 当成搜索引擎替代
- 把 Mongo 当成文档内容平台
- 把 Mongo 当成向量或 AI 检索底座

这些方向更可能落到：

- `getboot-search`
- `getboot-ai`
- 独立生态模块

## 11. 推荐的落地顺序

建议这样推进：

1. 先补这份方向文档，明确 MongoDB 不是对 `datasource` 的改名扩展
2. 再决定是否真的要新增 `getboot.database.mongodb.*` 配置子树
3. 如果要做实现，先只做 `infrastructure.mongodb.*` 的最小接入能力
4. 先验证 `MongoClient` / `MongoTemplate` / 启动校验 / 基础定制入口
5. 最后再评估是否需要稳定 `spi.mongodb`

原因：

- 先把边界讲清楚，能避免一上来把关系型语义改坏
- 先做最小接入层，能验证 Mongo 方向到底值不值得留在 `getboot-database`
- 扩展点后补，更符合“先稳定能力边界，再演进实现层”的仓库原则

## 12. 完成标准

`MongoDB` 方向这项 TODO 可以认为真正完成，至少满足：

- 已经明确 MongoDB 进入 `getboot-database` 的边界与非目标
- 如果新增实现，目录应落在 `infrastructure.mongodb.*`
- 关系型现有配置与能力语义没有被混淆
- README 能明确说明 MongoDB 是新增方向，而不是当前默认能力
- 文档已经写清楚配置规划、默认能力范围和与 `getboot-search` 等模块的边界
