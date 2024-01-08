package com.becoze.biback.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {
    @Bean
    public RedissonClient getRedisson(){
        // Create config object
        Config config = new Config();
        config.useSingleServer()
                .setDatabase(1)
                .setAddress("redis://127.0.0.1:6379")
                .setPassword("123456");

        // Sync and Async API
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
