package kr.hhplus.be.server.common.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합 테스트 기본 클래스 - 개선된 버전
 * 
 * 특징:
 * - @Sql 어노테이션으로 테스트 데이터 자동 정리
 * - 더 안전한 테스트 환경 제공
 * - 테스트 간 데이터 간섭 최소화
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Sql(scripts = "/test-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class IntegrationTestBase {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // 컨테이너 재사용으로 성능 최적화

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * 테스트 실행 전 상태 확인
     */
    protected void verifyTestEnvironment() {
        if (!mysql.isRunning()) {
            throw new IllegalStateException("MySQL 컨테이너가 실행되지 않았습니다.");
        }
    }

    /**
     * 테스트용 고유한 사용자 ID 생성
     * 테스트 간 데이터 충돌 방지
     */
    protected Long generateUniqueUserId() {
        return System.currentTimeMillis() % 100000 + 1000; // 1000~101000 범위
    }

    /**
     * 테스트용 고유한 상품 이름 생성
     */
    protected String generateUniqueProductName(String baseName) {
        return baseName + "_" + System.currentTimeMillis();
    }
}