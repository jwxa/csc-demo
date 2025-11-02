package com.github.jwxa.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RClientSideCaching;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.TrackingListener;
import org.redisson.api.options.ClientSideCachingOptions;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.Protocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class RedissonConfig {

    private final RedissonProperties redissonProperties;

    public RedissonConfig(RedissonProperties redissonProperties) {
        this.redissonProperties = redissonProperties;
    }

//    @Bean(destroyMethod = "shutdown") // 确保 Spring 关闭时释放资源
//    public RedissonClient redissonClient() {
//        Config config = new Config();
//        config.setProtocol(Protocol.RESP3);  // 指定使用 RESP3 协议
//        config.useSingleServer().setAddress(address) // 你的 Redis 地址
//                .setDatabase(0);
//        config.setCodec(new StringCodec());
//        return Redisson.create(config);
//    }


    @Bean(destroyMethod = "shutdown") // 确保 Spring 关闭时释放资源
    public RedissonClient redissonClientRedisCluster() {
        Config config = new Config();
        config.setProtocol(Protocol.RESP3);  // 指定使用 RESP3 协议
        RedissonProperties.ClusterServersConfig clusterConfig = redissonProperties.getClusterServersConfig();
        config.useClusterServers()
                .setPassword(clusterConfig.getPassword() == null || clusterConfig.getPassword().isEmpty() ? null : clusterConfig.getPassword())
                .addNodeAddress(clusterConfig.getNodeAddresses().toArray(new String[0]));
        config.setCodec(new StringCodec());
        return Redisson.create(config);
    }

    /**
     * redisson 本地缓存 localCachedMap 实现用的是PUB/SUB
     * 存放在redis里的是hash结构
     *
     * @param client
     * @return
     */
//    @Bean
//    public RLocalCachedMap<String, String> localCachedMap(RedissonClient client) {
//        LocalCachedMapOptions<String, String> options = LocalCachedMapOptions.<String, String>name("demoLocalMap").cacheSize(5).timeToLive(Duration.ofSeconds(60))
//                /**
//                 * 断线重连策略，当Redis连接断开后重新连接时的行为策略
//                 * 可选值：
//                 * - CLEAR: 清空本地缓存数据（默认）
//                 * - LOAD: 从Redis重新加载数据到本地缓存
//                 * - NONE: 不做任何处理，保留本地缓存数据
//                 */.reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR)
//                /**
//                 * 过期事件策略，处理缓存过期事件的方式
//                 * 可选值：
//                 * - SUBSCRIBE: 订阅过期事件频道
//                 * - SUBSCRIBE_WITH_KEYSPACE_CHANNEL: 订阅键空间通知频道（推荐）
//                 * - NONE: 不订阅任何过期事件
//                 */.expirationEventPolicy(LocalCachedMapOptions.ExpirationEventPolicy.SUBSCRIBE_WITH_KEYSPACE_CHANNEL)
//                /**
//                 * 缓存淘汰策略，当缓存达到最大容量时的淘汰算法
//                 * 可选值：
//                 * - LRU: 最近最少使用（默认）
//                 * - LFU: 最少频繁使用
//                 * - NONE: 不进行淘汰，达到上限后无法继续添加
//                 */.evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
//                .cacheProvider(LocalCachedMapOptions.CacheProvider.CAFFEINE)
//                ;
//
//        RLocalCachedMap<String, String> localCachedMap = client.getLocalCachedMap(options);
//        localCachedMap.addListener(new MapPutListener() {
//            @Override
//            public void onPut(String name) {
//                log.info("onPut key={}", name);
//            }
//        });
//        localCachedMap.addListener(new MapRemoveListener() {
//            @Override
//            public void onRemove(String name) {
//                log.info("onRemove key={}", name);
//            }
//        });
//        return localCachedMap;
//    }

//    @Bean
//    public RMapCache<String, String> rMapCache(RedissonClient client) {
//        RMapCache<String, String> demoMapCache = client.getMapCache("demoMapCache");
//        return demoMapCache;
//    }

//    /**
//     * Redis 6.0 提供的 Client Side Caching (CSC) 是按 key 维度通知的。
//     * 但是 Redisson 里如果对一个 RMap / RMapCache 启用 CSC，每次某个 entry 被修改，Redisson 会直接把整个 Map 的本地缓存都清空。
//     * @param redissonClient
//     * @return
//     */
//    @Bean
//    public RMap<Object, Object> demoMapCache(RedissonClient redissonClient) {
//        RClientSideCaching csc = redissonClient.getClientSideCaching(ClientSideCachingOptions.defaults());
//        RMap<Object, Object> demoMapCache = csc.getMap("demoMapCache", new StringCodec());
//        return demoMapCache;
//    }


    @Bean
    public RBucket<Object> demoBucket(RedissonClient redissonClient) {
        ClientSideCachingOptions options = ClientSideCachingOptions.defaults()
                .size(1000) // 缓存大小
                .evictionPolicy(ClientSideCachingOptions.EvictionPolicy.LRU) // 淘汰策略
                .timeToLive(Duration.ofMillis(60_000)) // 缓存有效期（毫秒）
                .maxIdle(Duration.ofMillis(120_000)); // 最大空闲时间（毫秒）
        RClientSideCaching csc = redissonClient.getClientSideCaching(options);
        RBucket<Object> demoBucket = csc.getBucket("demoBucket");
        // 启用跟踪监听器以便在外部更改时刷新本地缓存
         demoBucket.addListener(new TrackingListener() {
             @Override
             public void onChange(String name) {
                 log.info("tracking on change, key:{}", name);
             }
         });
        return demoBucket;
    }


}