package kr.hhplus.be.server.common.test;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;

/**
 * 통합 테스트 기본 클래스 - 호환성 문제 해결 완료 버전
 * 
 * 수정사항:
 * - withTmpFs Map 형태로 수정
 * - EntityManager 추가로 flush 기능 제공
 * - 안정적인 컨테이너 설정
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Transactional
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.jpa.defer-datasource-initialization=false",
        "logging.level.org.testcontainers=INFO"
})
public abstract class IntegrationTestBase {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                    "--default-authentication-plugin=mysql_native_password",
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci")
            .withStartupTimeout(Duration.ofMinutes(3))
            .withConnectTimeoutSeconds(180)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withTmpFs(Map.of("/var/lib/mysql", "rw,noexec,nosuid,size=512m")) // Map 형태로 수정
            .withReuse(false);

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected EntityManager entityManager; // flush 기능을 위한 EntityManager 추가

    /**
     * Spring Boot에 MySQL 연결 정보 동적 주입
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // 기본 데이터소스 설정
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // HikariCP 연결 풀 안정성 설정
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "1");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "300000");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "600000");

        // JPA/Hibernate 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");

        System.out.println("🔧 TestContainers Configuration Applied:");
        System.out.println("   JDBC URL: " + mysql.getJdbcUrl());
    }

    @BeforeAll
    static void beforeAll() {
        if (!mysql.isRunning()) {
            System.out.println("🚀 Starting MySQL container...");
            mysql.start();
        }

        if (!mysql.isRunning()) {
            throw new IllegalStateException("❌ MySQL 컨테이너 시작 실패!");
        }

        System.out.println("✅ MySQL Container Ready: " + mysql.getJdbcUrl());
        System.out.println("   Container ID: " + mysql.getContainerId());
    }

    @BeforeEach
    void setUp() {
        verifyTestEnvironment();
    }

    /**
     * 테스트 환경 검증
     */
    protected void verifyTestEnvironment() {
        if (!mysql.isRunning()) {
            throw new IllegalStateException("❌ MySQL 컨테이너가 실행되지 않았습니다.");
        }
    }

    /**
     * 테스트용 고유 ID 생성
     */
    protected Long generateUniqueUserId() {
        return System.currentTimeMillis() % 100000 + 1000;
    }

    protected String generateUniqueProductName(String baseName) {
        return baseName + "_" + System.currentTimeMillis();
    }

    /**
     * 즉시 DB 반영 (트랜잭션 문제 해결용)
     */
    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * 컨테이너 상태 로깅
     */
    protected void logContainerStatus() {
        System.out.println("=== TestContainers Status ===");
        System.out.println("Running: " + mysql.isRunning());
        System.out.println("Container ID: " + mysql.getContainerId());

        try {
            System.out.println("JDBC URL: " + mysql.getJdbcUrl());
            System.out.println("Mapped Port: " + mysql.getMappedPort(3306));
        } catch (Exception e) {
            System.out.println("Status Error: " + e.getMessage());
        }

        if (!mysql.isRunning()) {
            try {
                System.out.println("=== Container Logs ===");
                System.out.println(mysql.getLogs());
            } catch (Exception e) {
                System.out.println("로그 조회 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 테스트 실패 시 디버깅
     */
    protected void debugTestFailure(String testName, Exception e) {
        System.err.println("❌ Test Failed: " + testName);
        System.err.println("Error: " + e.getMessage());
        System.err.println("Error Type: " + e.getClass().getSimpleName());

        logContainerStatus();

        if (e.getCause() != null) {
            System.err.println("Root Cause: " + e.getCause().getMessage());
        }
    }
}