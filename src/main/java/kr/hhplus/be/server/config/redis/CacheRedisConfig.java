package kr.hhplus.be.server.config.redis;

import java.time.Duration;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 캐시용 Redis 설정
 * - 단순한 GET/SET 연산에 최적화
 * - Spring Boot와 완벽한 통합
 * - 가벼운 오버헤드
 */
@Configuration
@EnableCaching
public class CacheRedisConfig {

    private final ObjectMapper objectMapper;

    public CacheRedisConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.cluster.nodes:}")
    private List<String> clusterNodes;

    @Value("${spring.cache.redis.time-to-live:1800000}")
    private long defaultTtl;

    @Bean("cacheRedisConnectionFactory")
    @Primary
    public LettuceConnectionFactory cacheRedisConnectionFactory() {
        LettucePoolingClientConfiguration poolConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(new GenericObjectPoolConfig<>() {
                    {
                        setMaxTotal(8); // 캐시용은 적은 커넥션으로 충분
                        setMaxIdle(4);
                        setMinIdle(2);
                        setTestOnBorrow(true);
                        setTestOnReturn(true);
                        setTestWhileIdle(true);
                    }
                })
                .commandTimeout(Duration.ofMillis(2000))
                .build();

        // 클러스터 환경인지 확인
        if (clusterNodes != null && !clusterNodes.isEmpty()) {
            RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(clusterNodes);
            clusterConfig.setMaxRedirects(3);
            return new LettuceConnectionFactory(clusterConfig, poolConfig);
        } else {
            // 단일 인스턴스 (테스트 환경)
            RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
            return new LettuceConnectionFactory(standaloneConfig, poolConfig);
        }
    }

    @Bean("cacheRedisTemplate")
    @Primary
    public RedisTemplate<String, Object> cacheRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(cacheRedisConnectionFactory());

        // Jackson2JsonRedisSerializer 생성 - 타입 정보 보존 설정
        ObjectMapper mapper = objectMapper.copy();
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    public CacheManager cacheManager() {
        // Jackson2JsonRedisSerializer 생성 - 타입 정보 보존 설정
        ObjectMapper mapper = objectMapper.copy();
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(defaultTtl))
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(cacheRedisConnectionFactory())
                .cacheDefaults(config)
                .build();
    }
}