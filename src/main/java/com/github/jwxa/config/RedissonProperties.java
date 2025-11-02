package com.github.jwxa.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "redisson")
public class RedissonProperties {
    
    private String address = "redis://127.0.0.1:6379";
    
    private ClusterServersConfig clusterServersConfig = new ClusterServersConfig();
    
    @Data
    public static class ClusterServersConfig {
        private List<String> nodeAddresses;
        private String password;
    }
}