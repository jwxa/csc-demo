
# Redis Client-Side Caching 场景演练指南

This guide documents the `/scenario` controller and the playback endpoints that exercise production-like situations using **Redisson ClientSideCaching (CSC)**. Each section explains the goal, manual steps, HTTP calls, and suggested validations so the drills can be repeated easily.

---

## 1. Prerequisites / 环境准备

- Redis cluster: start with `redis-cluster-compose.yml`, initialise if needed via `README-redis-cluster.md`.
- Spring Boot app: default `RedissonConfig` (RESP3 + `StringCodec`), port `18080`.
- Visualizer: open `http://localhost:18080/scenario-visualizer.html`（若返回 404，可访问 `http://localhost:18080/scenario/visualizer`）。
- Recommended cleanup before each drill:
  ```bash
  redis-cli -c -h 127.0.0.1 -p 6379 DEL "scenario:csc-map"
  redis-cli -c -h 127.0.0.1 -p 6379 DEL "scenario:csc-bucket"
  redis-cli -c -h 127.0.0.1 -p 6379 DEL "scenario:csc-map-hash"
  ```

---

## 2. Scenario: CSC Invalidation / CSC 失效通知

- API：`POST /scenario/near-cache/invalidation`
- Visualizer：`Key 无效化`
- 步骤中新增 `eventual-check`，会再次读取本地缓存以展示最终与远端一致；若仍担心延迟，可调用 `POST /scenario/near-cache/status` 或在页面选择 `Near Cache 状态查询`。

---

## 3. Scenario: TTL Drift / TTL 漏斗

- API：`POST /scenario/near-cache/ttl-drift`
- Visualizer：`TTL 漏斗`
- 关注：`steps[2].observations` 中本地/远端差异（本地仍有值、远端为 `null`、TTL < 0）

---

## 4. Scenario: Expiration Policy Verification / 过期策略验证

- API：`POST /scenario/expire-policy`
- Visualizer：`TTL 过期策略验证`
- 参数：`key`, `value`, `ttlSeconds`, `pollIntervalMillis`, `maxWaitMillis`
- 输出：轮询样本 `samples` + 是否在窗口内失效

---

## 5. Scenario: Event Storm Simulation / 事件风暴模拟

- API：`POST /scenario/event-storm`
- Visualizer：`事件风暴模拟`
- 参数：`key`, `initialValue`, `iterations`, `pauseMillis`
- 输出：事件循环耗时、风暴后本地与远端值是否一致

---

## 6. Scenario: String Key Churn / 字符串批量震荡

- API：`POST /scenario/string-churn`
- Visualizer：`字符串 Key 批量震荡`
- 参数：`prefix`, `keyCount`, `iterations`, `payloadSize`, `pauseMillis`
- 输出：
  - `warmup`：初始化写入耗时及堆占用
  - `churn-loop.samples`：采样的迭代进度与堆使用量
  - `summary`：最终堆占用

---

## 7. Scenario: CSC Hash Invalidation / Hash 键失效同步

- API：`POST /scenario/near-cache/hash-invalidation`
- Visualizer：`Hash 字段无效化`
- 参数：`key`, `field`, `initialValue`, `updatedValue`, `awaitMillis`
- PlantUML：`docs/csc-hash-flow.puml`

---

## 8. Scenario: Cluster Topology Snapshot / 集群拓扑快照

- API：`POST /scenario/cluster/topology`
- Visualizer：`集群拓扑快照`
- 输出：master/replica 列表（地址、ping、server/cluster info）

---

## 9. Scenario: Replica Readiness / 读写分离检测

- API：`POST /scenario/cluster/replica-readiness`
- Visualizer：`读写分离检测`
- 输出：副本节点健康状态；副本缺失时会提示

---

## 10. Scenario: CSC During Failover / 主从切换下的 CSC

1. `POST /scenario/csc/warmup`
2. `GET /scenario/csc/state`
3. 手动停主节点（如 `docker stop redis-node-1`）
4. 故障期间重复 `GET /cscGet` 与 `GET /scenario/csc/state`
5. 恢复节点后再次 `GET /scenario/csc/state`

---

## 11. Troubleshooting Checklist / 排查清单

- **Map 名称**：`/scenario/near-cache/invalidation` 的 `context.mapName`
- **Hash 名称**：`/scenario/near-cache/hash-invalidation` 的 `context.hashName`
- **副本数**：`/scenario/cluster/replica-readiness` 的 `context.replicaCount`
- **监控指标**：`http://localhost:18080/actuator/metrics/cache.gets` 等
- **强制清理**：`redis-cli DEL scenario:csc-map`

---

## 12. Recommendations / 建议

1. 始终开启 `notify-keyspace-events Ex` 确保 CSC 无效化通知。
2. 协调 CSC TTL 与后端数据寿命，必要时刷新或写透。
3. 将故障切换、事件风暴等演练纳入回归，避免生产脏数据。
4. 整理为自动化测试（Postman、CI 脚本）以便重复验证。

定期演练可显著降低客户端缓存引发的生产风险。如需扩展更多场景（如批量写入、热点 key 限流），可以继续在 `NearCacheScenarioController` 与可视化页面中添加。
