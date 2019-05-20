package com.github.roger.cahce.config;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.github.roger.serializer.FastJsonRedisSerializer;
import org.github.roger.serializer.KryoRedisSerializer;
import org.github.roger.serializer.StringRedisSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@PropertySource({"classpath:application.properties"})
public class RedisConfig {

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.pool.max-idle:200}")
    private int maxIdle;

    @Value("${spring.redis.pool.min-idle:10}")
    private int minIdle;

    @Value("${spring.redis.pool.max-active:80}")
    private int maxActive;

    @Value("${spring.redis.pool.max-wait:-1}")
    private int maxWait;


//    @Bean
//    public JedisConnectionFactory redisConnectionFactory() {
//        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
//        jedisPoolConfig.setMinIdle(minIdle);
//        jedisPoolConfig.setMaxIdle(maxIdle);
//        jedisPoolConfig.setMaxTotal(maxActive);
//        jedisPoolConfig.setMaxWaitMillis(maxWait);
//
//        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(jedisPoolConfig);
//        jedisConnectionFactory.setDatabase(database);
//        jedisConnectionFactory.setHostName(host);
//        jedisConnectionFactory.setPassword(password);
//        jedisConnectionFactory.setPort(port);
//        jedisConnectionFactory.setUsePool(true);
//        return jedisConnectionFactory;
//    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        ClientResources clientResources = DefaultClientResources.create();
        LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
        builder.clientResources(clientResources);
        LettuceClientConfiguration configuration = builder.build();

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(RedisPassword.of(password));
        config.setDatabase(database);
        return new LettuceConnectionFactory(config, configuration);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        FastJsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJsonRedisSerializer<Object>(Object.class, "com.xxx.");
        KryoRedisSerializer<Object> kryoRedisSerializer = new KryoRedisSerializer<Object>(Object.class);

        // 设置值（value）的序列化采用FastJsonRedisSerializer。
        redisTemplate.setValueSerializer(kryoRedisSerializer);
        redisTemplate.setHashValueSerializer(kryoRedisSerializer);
        // 设置键（key）的序列化采用StringRedisSerializer。
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
