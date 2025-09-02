package kr.hhplus.be.server.config.redis;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Redis 설정 Properties
 * 
 * YAML 배열을 안전하게 읽기 위한 @ConfigurationProperties 방식
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {
    
    /**
     * 클러스터 노드 목록
     */
    private List<String> clusterNodes = new ArrayList<>();
    
    /**
     * 단일 호스트 (클러스터가 아닐 때 사용)
     */
    private String host = "localhost";
    
    /**
     * 단일 포트 (클러스터가 아닐 때 사용)
     */
    private int port = 6379;
    
    /**
     * 클러스터 모드가 활성화되었는지 확인
     */
    public boolean isClusterMode() {
        return clusterNodes != null && !clusterNodes.isEmpty();
    }
}