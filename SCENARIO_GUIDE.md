# Redis Client-Side Caching 场景演练指南

This guide documents the new `/scenario` controller that exercises production-like situations using **Redisson ClientSideCaching (CSC)**. Each section explains the goal, manual steps, HTTP calls, and suggested validations so you can repeat the drills whenever needed.  
本指南围绕新增的 `/scenario` 控制器，使用 **Redisson ClientSideCaching (CSC)** 复现常见生产场景。每一节都会说明目标、操作步骤、调用接口与校验方式，方便重复演练。

---

## 1. Prerequisites / 环境准备

- Redis cluster: start it with `redis-cluster-compose.yml` and, if required, follow `README-redis-cluster.md` to run the initialization command.  
  Redis 集群：使用 `redis-cluster-compose.yml` 启动，必要时按 `README-redis-cluster.md` 初始化。
- Spring Boot app: default `RedissonConfig` (RESP3 + `StringCodec`), port `18080`.  
  Spring Boot 应用：使用默认 `RedissonConfig`（RESP3 + `StringCodec`），端口 `18080`。
- Scenario endpoints: `NearCacheScenarioController` (`/scenario/**`), fully isolated from the existing demo APIs.  
  场景入口：`NearCacheScenarioController`（`/scenario/**`），与现有演示接口相互隔离。
- Visualization: open `http://localhost:18080/scenario-visualizer.html` (or `/scenario/visualizer`) for animated step playback.  
  可访问 `http://localhost:18080/scenario-visualizer.html`（若仍报 404，可换用 `http://localhost:18080/scenario/visualizer`）查看动画演练页。
- Recommended cleanup before running the drills:  
  演练前建议清理以下 key：
  ```bash
  redis-cli -c -h 127.0.0.1 -p 6379 DEL "scenario:csc-map"
  redis-cli -c -h 127.0.0.1 -p 6379 DEL "scenario:csc-bucket"
  redis-cli -c -h 127.0.0.1 -p 6379 DEL "scenario:csc-map-hash"
  ```

---

## 2. Scenario: CSC Invalidation / CSC 失效通知

**Goal** - Prove that when another instance updates Redis directly, the local CSC map receives tracking messages and refreshes the cached value.  
**目标** - 验证当其他实例直接写入 Redis 时，本地 CSC map 能否收到追踪消息并刷新缓存。

**Steps / 操作步骤**
1. Call `POST /scenario/near-cache/invalidation` with the initial value.  
   调用 `POST /scenario/near-cache/invalidation` 写入初始值。
2. The service issues a plain `RMap` write to mimic "another node" changing Redis.  
   服务内部通过 `RMap` 写入更新值，模拟“另一节点”改动 Redis。
3. Inspect the `steps` array in the response to confirm `local` and `remote` match after the wait window.  
   查看响应中的 `steps`，确认等待窗口后 `local` 与 `remote` 一致。

**API**
- Endpoint: `POST /scenario/near-cache/invalidation`
- Sample request / 示例请求:
  ```json
  {
    "key": "user:128",
    "initialValue": "{\"name\":\"alpha\",\"version\":1}",
    "updatedValue": "{\"name\":\"alpha\",\"version\":2}",
    "awaitMillis": 500
  }
  ```
- Notable fields / 关注字段:
  - `scenarioCode`: `near-cache-csc-invalidation`
  - `context.mapName`: Redis hash backing the CSC map (`scenario:csc-map`)
  - `steps[3].observations.local`: Expected to reflect the updated value

**Variations / 变体测试**
- Disable `notify-keyspace-events` temporarily to understand how tracking dependencies affect CSC.  
  暂时关闭 `notify-keyspace-events` 观察缺少追踪消息时的行为。
- Reduce `awaitMillis` to observe behaviour under very frequent changes.  
  减小 `awaitMillis` 以模拟高频更新下的同步效果。

---

## 3. Scenario: TTL Drift / TTL 漏斗

**Goal** - Simulate the situation where the Redis entry expires earlier than the local CSC TTL.  
**目标** - 模拟 Redis entry 先于 CSC 本地 TTL 过期的风险。

**Steps / 操作步骤**
1. Call `POST /scenario/near-cache/ttl-drift`.  
   调用 `POST /scenario/near-cache/ttl-drift`。
2. The service applies an expiry to the Redis map entry.  
   服务端为 Redis 中的 key 设置过期时间。
3. After `waitMillis`, compare `local` versus `remote` and review `remainTimeToLiveMillis`.  
   等待 `waitMillis` 后对比 `local` 与 `remote`，关注 `remainTimeToLiveMillis`。

**API**
- Endpoint: `POST /scenario/near-cache/ttl-drift`
- Sample request / 示例请求:
  ```json
  {
    "key": "inventory:sku-1001",
    "value": "stock-12",
    "redisTtlSeconds": 3,
    "waitMillis": 6000
  }
  ```
- Notable fields / 关注字段:
  - `scenarioCode`: `ttl-drift-between-local-and-redis`
  - `steps[2].observations.local`: 本地 CSC 缓存值
  - `steps[2].observations.remote`: 远端值（TTL 过期后预计为 `null`）
  - `steps[2].observations.remainTimeToLiveMillis`: `-2` 表示 Redis 已删除该 entry

**Notes / 补充说明**
- If `local` still holds the old value, CSC has not yet invalidated locally. Combine with the invalidation scenario to determine whether additional eviction hooks are necessary.  
  若 `local` 仍为旧值，说明 CSC 本地尚未失效，可结合失效通知场景判断是否需要额外失效策略。
- Adjust `redisTtlSeconds` and `waitMillis` to explore borderline timings.  
  调整 `redisTtlSeconds` 与 `waitMillis` 验证不同超时时间组合。

---

## 4. Scenario: CSC Hash Invalidation / Map 键失效同步

**Goal** - Observe how CSC handles hash-style keys (e.g., Redis map entries) when another node mutates a specific field.  
**目标** - 验证当其他节点直接更新 Hash 字段时，CSC 是否能够刷新本地缓存。

**Steps / 操作步骤**
1. Call `POST /scenario/near-cache/hash-invalidation` with `key`, `field`, and initial value.  
   调用 `POST /scenario/near-cache/hash-invalidation`，提供 `key`、`field` 及初始值。
2. Service writes the hash entry via CSC map to build the local cache.  
   服务通过 CSC map 写入 Hash，构建本地缓存。
3. The service mimics another node by writing the same hash field through a non-CSC Redis client.  
   服务模拟其他节点直接使用非 CSC 客户端修改该 Hash 字段。
4. After `awaitMillis`, compare local and remote maps to check whether the updated field is synchronized.  
   等待 `awaitMillis` 后对比本地与远端的 Hash 值，确认字段同步。

**API**
- Endpoint: `POST /scenario/near-cache/hash-invalidation`
- Sample request / 示例请求:
  ```json
  {
    "key": "profile:2001",
    "field": "email",
    "initialValue": "alpha@example.com",
    "updatedValue": "beta@example.com",
    "awaitMillis": 500
  }
  ```
- Notable fields / 关注字段:
  - `scenarioCode`: `near-cache-csc-hash-invalidation`
  - `steps[*].observations.local`: Hash 在本地 CSC 中的状态
  - `steps[*].observations.remote`: Redis 真正存储的 Hash 状态
  - `context.field`: 本次关注的字段名

**Visualization / 交互流程图**
- Diagram source: `docs/csc-hash-flow.puml`
- Render command:
  ```bash
  plantuml docs/csc-hash-flow.puml
  ```
  生成的时序图展示了客户端调用、CSC map 本地命中与 Redis Cluster 之间的 PUB/SUB 失效过程。

---

## 5. Scenario: CSC During Failover / 主从切换下的 CSC

**Goal** - Check how the CSC bucket behaves during Redis primary failure or a transient network break.  
**目标** - 观察 Redis 主从切换或网络闪断期间 CSC bucket 的读写表现。

**Steps / 操作步骤**
1. `POST /scenario/csc/warmup` to seed the bucket and capture the baseline TTL.  
   `POST /scenario/csc/warmup` 预热 bucket，记录初始 TTL。
2. `GET /scenario/csc/state` to snapshot the current local value.  
   `GET /scenario/csc/state` 获取本地缓存快照。
3. Trigger failover, for example `docker stop redis-node-1`.  
   触发主节点故障，如执行 `docker stop redis-node-1`。
4. While the node is down, repeatedly call:  
   节点宕机期间循环调用：
   - `GET /cscGet` (existing API) to see if business reads stay warm.  
     `GET /cscGet`（既有接口）确认业务读是否命中本地缓存。
   - `GET /scenario/csc/state` to monitor `remainingTtlMillis` and `localValue`.  
     `GET /scenario/csc/state` 观察 `remainingTtlMillis` 与 `localValue`。
5. Bring the node back online and call `state` again to verify consistency.  
   恢复节点后再次调用 `state` 确认数据一致性。

**API**
- Warmup request / 预热示例:
  ```json
  {
    "value": "failover-checkpoint-001",
    "ttlSeconds": 45
  }
  ```
- Observability: `GET /scenario/csc/state`  
  观测接口：`GET /scenario/csc/state`
- Combine with baseline endpoints:  
  可配合原有接口：
  - `POST /cscSet?value=xxx`
  - `GET /cscGet`

**What to watch / 关注点**
- `remainingTtlMillis`: still decreases even when Redis is unavailable, showing the local timer keeps running.  
  `remainingTtlMillis` 会继续递减，说明本地 TTL 计时没有中断。
- Application logs tagged with `[ScenarioNearCacheConfig]` expose tracking callbacks that invalidate the cache after failover.  
  控制台中 `[ScenarioNearCacheConfig]` 日志展示追踪回调，可确认故障恢复后的失效动作。

---

## 6. Troubleshooting Checklist / 排查清单

- **Map name**: returned in `context.mapName` from the invalidation scenario.  
  **Map 名称**：可通过失效通知场景的 `context.mapName` 获取。
- **Metrics**: enable Actuator and visit `http://localhost:18080/actuator/metrics/cache.gets` or related metrics to gauge hit ratios.  
  **监控指标**：开启 Actuator 后访问 `http://localhost:18080/actuator/metrics/cache.gets` 了解命中率。
- **Force eviction**: `redis-cli DEL scenario:csc-map` clears both remote data and the associated CSC cache.  
  **强制清理**：执行 `redis-cli DEL scenario:csc-map` 同步清空远端与本地缓存。

---

## 7. Recommendations / 建议

1. Keep Redis keyspace notifications enabled (`notify-keyspace-events Ex`) so CSC receives invalidation events.  
   Redis 必须开启 `notify-keyspace-events Ex`，确保 CSC 能收到无效化事件。
2. Align CSC TTL with backend data lifetime; refresh or rewrite-through when the backend value changes frequently.  
   协调 CSC TTL 与后端数据生命周期，必要时刷新或写透。
3. Include CSC failover drills in your release checklist to avoid serving stale data during real incidents.  
   在发布前纳入 CSC 故障演练，避免故障时返回陈旧数据。
4. Convert this guide into automated scripts (Postman collection, integration tests) for regression coverage.  
   将演练流程转化为自动化脚本（Postman、集成测试等）以便持续回归。

Following these drills regularly reduces the operational risk of adopting client-side caching. If you need more scenarios (e.g., batch updates, hot-key throttling), extend `NearCacheScenarioController` with additional endpoints without touching existing business APIs.  
定期执行这些演练可以降低客户端缓存带来的生产风险。如需更多场景（例如批量写入、热点 key 限流），可在 `NearCacheScenarioController` 中继续扩展独立接口。
