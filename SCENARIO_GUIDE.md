
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
  ```

---

## 2. Scenario: CSC Invalidation / CSC 失效通知

- API：`POST /scenario/near-cache/invalidation`
- Visualizer：`Key 无效化`
- `eventual-check` 会在等待后重读一次，并在必要时 `readAllMap()` 强制刷新，以展示最终与远端一致。如需手动核对，可调用 `POST /scenario/near-cache/status` 或在页面选择 `Near Cache 状态查询`。

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

## 7. Scenario: CSC String Warmup / 字符串新增-更新-刷新

- API：`POST /scenario/csc/warmup`
- Visualizer：`CSC String 新增/更新/刷新`
- 参数：`initialValue`, `updatedValue`, `ttlSeconds`, `awaitMillis`, `refreshTtl`, `refreshTtlSeconds`
- 步骤说明：
  1. **warmup-local**：通过 CSC 客户端写入字符串并构建本地缓存，记录 TTL。
  2. **baseline-remote**：对比 Redis 中的远端字符串与 TTL。
  3. **remote-update**：模拟其他节点更新字符串，引发 CSC 失效通知。
  4. **verify-after-update**：等待 `awaitMillis` 后，再次读取字符串确认本地缓存是否同步刷新。
  5. **refresh-ttl**（可选）：若 `refreshTtl=true`，通过重新设置过期时间 + `touch` 刷新剩余 TTL，观察刷新前后的 TTL 变化。
- 若需要结合故障演练，可在步骤 3 后执行 Redis 主从切换，再通过 `GET /scenario/csc/state` 验证缓存与远端一致性。
- PlantUML：`docs/csc-string-warmup.puml`

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
