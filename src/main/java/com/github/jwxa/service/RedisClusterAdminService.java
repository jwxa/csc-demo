package com.github.jwxa.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.ClusterNode;
import org.redisson.api.ClusterNodesGroup;
import org.redisson.api.NodeType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisClusterAdminService {

    private final RedissonClient redissonClient;
    private final ShellService shellService;

    private static final String COMPOSE_FILE = "redis-cluster-compose.yml";

    public ClusterStatus status() {
        try {
            ClusterNodesGroup group = redissonClient.getClusterNodesGroup();
            List<NodeStatus> nodes = new ArrayList<>();
            for (ClusterNode n : group.getNodes(NodeType.MASTER)) {
                nodes.add(nodeStatus(n));
            }
            for (ClusterNode n : group.getNodes(NodeType.SLAVE)) {
                nodes.add(nodeStatus(n));
            }
            long masterCount = nodes.stream().filter(ns -> "MASTER".equals(ns.getNodeType())).count();
            long replicaCount = nodes.stream().filter(ns -> "SLAVE".equals(ns.getNodeType())).count();
            return ClusterStatus.builder()
                    .online(true)
                    .masterCount((int) masterCount)
                    .replicaCount((int) replicaCount)
                    .nodes(nodes)
                    .build();
        } catch (Exception e) {
            // When cluster is down or unreachable.
            log.warn("Cluster status check failed: {}", e.getMessage());
            return ClusterStatus.builder()
                    .online(false)
                    .masterCount(0)
                    .replicaCount(0)
                    .error(e.getClass().getSimpleName() + ": " + e.getMessage())
                    .nodes(List.of())
                    .build();
        }
    }

    public ShellService.ExecResult start() {
        // docker compose up -d
        return shellService.dockerCompose(COMPOSE_FILE, "up", "-d");
    }

    public ShellService.ExecResult stop() {
        // docker compose down
        return shellService.dockerCompose(COMPOSE_FILE, "down");
    }

    private NodeStatus nodeStatus(ClusterNode node) {
        String addr = String.valueOf(node.getAddr());
        String type = node.getType().name();
        Object ping;
        try {
            ping = node.ping();
        } catch (Exception e) {
            ping = false;
        }
        return new NodeStatus(addr, type, ping);
    }

    @Data
    @Builder
    public static class ClusterStatus {
        private boolean online;
        private int masterCount;
        private int replicaCount;
        private List<NodeStatus> nodes;
        private String error;
    }

    @Data
    public static class NodeStatus {
        private final String address;
        private final String nodeType;
        private final Object ping;
    }
}
