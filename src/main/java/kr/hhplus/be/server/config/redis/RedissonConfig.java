package kr.hhplus.be.server.config.redis;

import java.util.List;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 분산락용 Redisson 설정
 * - 검증된 분산락 구현 (Redlock 알고리즘)
 * - Pub/Sub 기반 효율적인 대기
 * - 자동 재시도 및 공정성 보장
 * - 다양한 락 타입 지원 (공정락, 읽기/쓰기 락)
 * - 락용은 더 많은 커넥션 필요 (경합 상황 대응)
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.cluster.nodes:}")
    private List<String> clusterNodes;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // 클러스터 환경인지 확인
        if (clusterNodes != null && !clusterNodes.isEmpty()) {
            // 클러스터 설정 - 락용은 16개 커넥션으로 경합 상황 대응
            config.useClusterServers()
                    .addNodeAddress(clusterNodes.stream()
                            .map(node -> "redis://" + node)
                            .toArray(String[]::new))
                    .setMasterConnectionPoolSize(16) // 마스터 커넥션 풀: 16개
                    .setSlaveConnectionPoolSize(16) // 슬레이브 커넥션 풀: 16개
                    .setMasterConnectionMinimumIdleSize(8) // 마스터 최소 유지: 8개
                    .setSlaveConnectionMinimumIdleSize(8) // 슬레이브 최소 유지: 8개
                    .setTimeout(3000)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500);
        } else {
            // 단일 인스턴스 설정 (테스트 환경)
            config.useSingleServer()
                    .setAddress("redis://" + redisHost + ":" + redisPort)
                    .setConnectionPoolSize(16) // 락용 커넥션 풀: 16개
                    .setConnectionMinimumIdleSize(8) // 최소 유지 커넥션: 8개
                    .setTimeout(3000)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500)
                    .setConnectTimeout(10000);
        }

        return Redisson.create(config);
    }
}