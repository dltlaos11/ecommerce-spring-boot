package kr.hhplus.be.server.config.redis;

import java.util.Map;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;

/**
 * 분산락용 Redisson 설정
 * - 검증된 분산락 구현 (Redlock 알고리즘)
 * - Pub/Sub 기반 효율적인 대기
 * - 자동 재시도 및 공정성 보장
 * - 다양한 락 타입 지원 (공정락, 읽기/쓰기 락)
 * - 락용은 더 많은 커넥션 필요 (경합 상황 대응)
 */
@Configuration
@RequiredArgsConstructor
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private final RedisProperties redisProperties;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        if (redisProperties.isClusterMode()) {
            // 클러스터 설정 - 락용은 16개 커넥션으로 경합 상황 대응
            String[] redisUrls = redisProperties.getClusterNodes().stream()
                    .map(node -> "redis://" + node)
                    .toArray(String[]::new);

            config.useClusterServers()
                    .addNodeAddress(redisUrls)
                    .setMasterConnectionPoolSize(16) // 마스터 커넥션 풀: 16개
                    .setSlaveConnectionPoolSize(16) // 슬레이브 커넥션 풀: 16개
                    .setMasterConnectionMinimumIdleSize(8) // 마스터 최소 유지: 8개
                    .setSlaveConnectionMinimumIdleSize(8) // 슬레이브 최소 유지: 8개
                    .setTimeout(3000)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500)
                    .setCheckSlotsCoverage(false) // 불완전한 클러스터에서도 작동
                    // Docker 내부 IP를 외부 접근 가능한 IP로 매핑 (NAT)
                    .setNatMap(Map.of(
                        "172.18.0.2:7001", "127.0.0.1:7001",
                        "172.18.0.3:7002", "127.0.0.1:7002", 
                        "172.18.0.4:7003", "127.0.0.1:7003",
                        "172.18.0.5:7004", "127.0.0.1:7004",
                        "172.18.0.6:7005", "127.0.0.1:7005",
                        "172.18.0.7:7006", "127.0.0.1:7006"
                    ));
        } else {
            // 단일 인스턴스 설정 (테스트 환경)
            config.useSingleServer()
                    .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
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