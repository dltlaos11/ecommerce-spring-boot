package kr.hhplus.be.server.common.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합 테스트 기본 클래스 - 핵심 문제만 수정
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@Transactional // 트랜잭션 복원 - 테스트 격리를 위해 필요
public abstract class IntegrationTestBase {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withCommand("--default-authentication-plugin=mysql_native_password")
            .withEnv("MYSQL_ROOT_PASSWORD", "test")
            .withReuse(false) // 일단 재사용 비활성화
            .withStartupTimeout(java.time.Duration.ofMinutes(2)); // 시작 타임아웃 증가

    // @DynamicPropertySource
    // static void configureProperties(DynamicPropertyRegistry registry) {
    // registry.add("spring.datasource.url", mysql::getJdbcUrl);
    // registry.add("spring.datasource.username", mysql::getUsername);
    // registry.add("spring.datasource.password", mysql::getPassword);
    // registry.add("spring.datasource.driver-class-name", () ->
    // "com.mysql.cj.jdbc.Driver");

    // // 🔧 HikariCP 연결 풀 최적화 - 핵심!
    // registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10");
    // registry.add("spring.datasource.hikari.minimum-idle", () -> "2");
    // registry.add("spring.datasource.hikari.connection-timeout", () -> "20000");
    // registry.add("spring.datasource.hikari.idle-timeout", () -> "300000");
    // }

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * 테스트 실행 전 상태 확인
     */
    protected void verifyTestEnvironment() {
        if (!mysql.isRunning()) {
            throw new IllegalStateException("MySQL 컨테이너가 실행되지 않았습니다.");
        }
        System.out.println("✅ MySQL Container running: " + mysql.getJdbcUrl());
    }

    /**
     * 테스트용 고유한 사용자 ID 생성
     */
    protected Long generateUniqueUserId() {
        return System.currentTimeMillis() % 100000 + 1000;
    }

    /**
     * 테스트용 고유한 상품 이름 생성
     */
    protected String generateUniqueProductName(String baseName) {
        return baseName + "_" + System.currentTimeMillis();
    }

    /**
     * 컨테이너 상태 확인 메서드
     */
    protected void logContainerStatus() {
        System.out.println("MySQL Container Status: " + mysql.isRunning());
        System.out.println("MySQL Container URL: " + mysql.getJdbcUrl());
        if (!mysql.isRunning()) {
            System.out.println("MySQL Container Logs: ");
            System.out.println(mysql.getLogs());
        }
    }
}