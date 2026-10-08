package com.tradeflow.market_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.url:${REDIS_URL:redis://localhost:6379}}")
    private String redisUrl;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        try {
            URI uri = new URI(redisUrl);
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(uri.getHost());
            config.setPort(uri.getPort());
            if (uri.getUserInfo() != null) {
                String[] auth = uri.getUserInfo().split(":", 2);
                if (auth.length == 2) {
                    config.setPassword(auth[1]);
                } else {
                    config.setPassword(auth[0]);
                }
            }

            if (redisUrl.startsWith("rediss://")) {
                LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                        .useSsl().disablePeerVerification().build();
                return new LettuceConnectionFactory(config, clientConfig);
            }

            return new LettuceConnectionFactory(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Redis URL", e);
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        return template;
    }
}