package kr.hhplus.be.server.config.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 분산락용 Redisson 설정
 * - 검증된 분산락 구현 (Redlock 알고리즘)
 * - Pub/Sub 기반 효율적인 대기
 * - 자동 재시도 및 공정성 보장
 * - 다양한 락 타입 지원 (공정락, 읽기/쓰기 락)
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
            // 클러스터 설정
            config.useClusterServers()
                    .addNodeAddress(clusterNodes.stream()
                            .map(node -> "redis://" + node)
                            .toArray(String[]::new))
                    .setTimeout(3000)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500);
        } else {
            // 단일 인스턴스 설정 (테스트 환경)
            config.useSingleServer()
                    .setAddress("redis://" + redisHost + ":" + redisPort)
                    .setTimeout(3000)
                    .setRetryAttempts(3)
                    .setRetryInterval(1500)
                    .setConnectTimeout(10000);
        }

        return Redisson.create(config);
    }
}