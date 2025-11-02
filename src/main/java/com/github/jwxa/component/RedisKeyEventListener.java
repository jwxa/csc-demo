package com.github.jwxa.component;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

//@Component
@Slf4j
public class RedisKeyEventListener {

    private final RedissonClient redissonClient;
    private final CaffeineCacheInvalidator invalidator;

    /**
     * 需要redis启动命令开启  notify-keyspace-events
     *
     * 键空间通知（Keyspace Notification）：以 __keyspace@<db>__ 为前缀，关注特定键的变化。
     * 键事件通知（Keyevent Notification）：以 __keyevent@<db>__ 为前缀，关注特定事件类型。
     * 例如，当对数据库 0 的键 mykey 执行 DEL 操作时，会触发以下两条消息：
     *
     * PUBLISH __keyspace@0__:mykey del
     * PUBLISH __keyevent@0__:del mykey
     *
     * @param redissonClient
     * @param invalidator
     */
    public RedisKeyEventListener(RedissonClient redissonClient, CaffeineCacheInvalidator invalidator) {
        this.redissonClient = redissonClient;
        this.invalidator = invalidator;

        // 订阅所有 key 过期事件
        RTopic expiredTopic = redissonClient.getTopic("__keyevent@0__:expired");
        expiredTopic.addListener(String.class, (channel, key) -> {
            log.info("监听到redis过期事件, key:{}", key);
//            invalidator.invalidate(key);
        });

        // 订阅所有 key 删除事件
        RTopic deleteTopic = redissonClient.getTopic("__keyevent@0__:del");
        deleteTopic.addListener(String.class, (channel, key) -> {
            log.info("监听到redis删除事件, key:{}", key);
//            invalidator.invalidate(key);
        });

        //注意 监听器和localMap的监听器不能同时监听，否则会冲突
        //        RTopic hashDelTopic = redissonClient.getTopic("__keyevent@*:hdel");
        //        hashDelTopic.addListener(String.class, (channel, message) -> {
        //            log.info("监听到redis hdel事件, channel:{}, message:{}", channel, message);
        //        });
    }
}