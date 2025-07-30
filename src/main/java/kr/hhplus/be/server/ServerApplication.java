package kr.hhplus.be.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 변경사항:
 * - exclude 제거 (JPA 활성화)
 * - @EnableJpaAuditing 추가 (자동 시간 필드 관리)
 */
@SpringBootApplication // exclude 제거 - JPA 활성화
@EnableJpaAuditing // 🆕 JPA Auditing 활성화
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}