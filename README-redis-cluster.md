# Redis 集群部署与演练指南

本文档基于 `redis-cluster-compose.yml` 的实际配置，说明如何在本项目中启动与验证 6 节点 Redis Cluster，并给出常见演练及排查建议。

---

## 1. 集群拓扑 / Cluster Topology

- 节点组成：`redis-node-1` ~ `redis-node-6`，共 6 个容器  
  - 端口规划：分别监听 `7201` ~ `7206`（客户端端口），并开放 `7301` ~ `7306`（总线端口）  
  - 网络模式：全部使用 `network_mode: host`，可在宿主机直接以 `127.0.0.1:<端口>` 访问  
  - 认证策略：`redis.sh` 中启用 `requirepass pass@123`，因此所有读写都须携带密码
- 配置脚本：所有节点通过挂载的 `redis.sh` 生成 `redis.conf`，开启集群模式、`notify-keyspace-events Ex`、关闭保护模式
- 集群初始化：`redis-cluster-creator` 服务在所有节点就绪后执行  
  ```bash
  redis-cli -a 'pass@123' --cluster create \
    host.docker.internal:7201 host.docker.internal:7202 host.docker.internal:7203 \
    host.docker.internal:7204 host.docker.internal:7205 host.docker.internal:7206 \
    --cluster-replicas 1 --cluster-yes
  ```  
  该命令将前 3 个节点设为主节点，后 3 个节点作为其副本，适用于 Windows / macOS Docker Desktop 环境；在 Linux 主机中可将 `host.docker.internal` 改为 `127.0.0.1`。

---

## 2. 启动流程 / How to Start

1. **启动容器**  
   ```bash
   docker compose -f redis-cluster-compose.yml up -d
   ```  
   首次执行时会启动 6 个 Redis 节点与 1 个 `redis-cluster-creator` 容器。后续再次启动可忽略 `creator` 的报错（已完成集群初始化）。

2. **确认所有节点运行**  
   ```bash
   docker ps --filter "name=redis-node"
   ```  
   需看到 `redis-node-1` ~ `redis-node-6` 全部为 `Up` 状态。

3. **验证集群状态**  
   ```bash
   redis-cli -c -h 127.0.0.1 -p 7201 -a pass@123 cluster info
   redis-cli -c -h 127.0.0.1 -p 7201 -a pass@123 cluster nodes
   ```  
   `cluster_state:ok` 且 6 个节点均显示 `master` / `slave` 角色即表示成功。

---

## 3. 常用连接方式 / Connecting to the Cluster

```bash
# 连接主节点示例
redis-cli -c -h 127.0.0.1 -p 7201 -a pass@123

# 连接任意副本
redis-cli -c -h 127.0.0.1 -p 7205 -a pass@123
```

> 提示：`-c` 参数启用集群模式，可自动重定向到对应槽位；如在容器内部执行，需要将主机地址改为 `host.docker.internal`（Windows/macOS）或 `172.17.0.1`（默认 Docker Linux bridge）。

---

## 4. 主从切换演练 / Failover Drill

1. **写入示例数据**  
   ```bash
    redis-cli -c -h 127.0.0.1 -p 7201 -a pass@123 set demo:key value-1
   ```
2. **模拟主节点故障**（以 `redis-node-1` 为例）  
   ```bash
   docker stop redis-node-1
   ```
3. **观察自动故障转移**  
   ```bash
   redis-cli -c -h 127.0.0.1 -p 7202 -a pass@123 cluster nodes
   redis-cli -c -h 127.0.0.1 -p 7202 -a pass@123 get demo:key
   ```
   可看到其对应副本被提升为新主节点，槽位仍然可读。
4. **恢复原节点**  
   ```bash
   docker start redis-node-1
   redis-cli -c -h 127.0.0.1 -p 7201 -a pass@123 cluster nodes
   ```  
   原主节点会自动以从节点角色重新加入。

---

## 5. 常见问题 / Troubleshooting

| 问题 | 处理方式 |
| ---- | -------- |
| `redis-cli` 连接被拒绝 | 确认使用 `-a pass@123`，并检查容器是否已启动 |
| `cluster_state:fail` | 使用 `docker compose down` 后再 `docker compose up -d` 完整重启或重新执行创建命令 |
| 主节点未自动切换 | 确认 `redis-cluster-creator` 创建的拓扑未被手动修改，可通过 `cluster reset` 清理后重新创建 |
| Windows/WSL 无法访问 | 因 `network_mode: host`，请在同一环境中启动与访问（Windows 上建议直接使用 PowerShell / CMD；WSL 中运行需替换访问 IP） |

---

## 6. 清理环境 / Tear Down

```bash
docker compose -f redis-cluster-compose.yml down
docker volume prune  # 如需清除数据文件，可选择执行
```

---

通过上述流程即可快速搭建与验证与 `redis-cluster-compose.yml` 完全一致的 Redis Cluster 环境，并结合项目中的近端缓存场景演练进行联调测试。
