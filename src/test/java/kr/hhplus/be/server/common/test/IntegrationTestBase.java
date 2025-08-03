package kr.hhplus.be.server.common.test;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합 테스트 기본 클래스 - 완전 수정 버전
 * 
 * 주요 수정사항:
 * - TestContainers 활성화
 * - 안정적인 컨테이너 시작 보장
 * - 데이터 격리 개선
 * - 에러 처리 강화
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers // ✅ TestContainers 활성화
@Transactional
@Sql(scripts = "/test-data-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class IntegrationTestBase {

    // ✅ 싱글톤 컨테이너 - static 초기화로 안정성 확보
    private static final MySQLContainer<?> mysql;

    static {
        mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withCommand("--default-authentication-plugin=mysql_native_password")
                .withStartupTimeout(Duration.ofMinutes(5)) // 타임아웃 증가
                .withConnectTimeoutSeconds(300)
                .withReuse(true); // 컨테이너 재사용

        // 즉시 시작
        mysql.start();

        if (!mysql.isRunning()) {
            throw new IllegalStateException("❌ MySQL 컨테이너 시작 실패!");
        }

        System.out.println("✅ MySQL Container started successfully: " + mysql.getJdbcUrl());
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * ✅ Spring Boot에 DB 설정 동적 주입
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL 연결 정보
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // HikariCP 최적화
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "1");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");
        registry.add("spring.datasource.hikari.idle-timeout", () -> "600000");

        // JPA 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");

        // DataLoader 비활성화
        registry.add("spring.sql.init.mode", () -> "never");

        System.out.println("🔧 DB Configuration applied: " + mysql.getJdbcUrl());
    }

    @BeforeAll
    static void beforeAll() {
        if (!mysql.isRunning()) {
            throw new IllegalStateException("❌ MySQL 컨테이너가 실행되지 않았습니다.");
        }
        System.out.println("✅ Test environment ready: " + mysql.getJdbcUrl());
    }

    @BeforeEach
    void setUp() {
        verifyTestEnvironment();
    }

    protected void verifyTestEnvironment() {
        if (!mysql.isRunning()) {
            throw new IllegalStateException("❌ MySQL 컨테이너가 실행되지 않았습니다.");
        }
        // System.out.println("🔍 Test environment verified");
    }

    /**
     * 테스트용 고유 ID 생성 (충돌 방지)
     */
    protected Long generateUniqueUserId() {
        return System.currentTimeMillis() % 100000 + 1000;
    }

    protected String generateUniqueProductName(String baseName) {
        return baseName + "_" + System.currentTimeMillis();
    }

    /**
     * 컨테이너 상태 로깅 (디버깅용)
     */
    protected void logContainerStatus() {
        System.out.println("=== Container Status ===");
        System.out.println("Running: " + mysql.isRunning());
        System.out.println("JDBC URL: " + mysql.getJdbcUrl());
        System.out.println("Username: " + mysql.getUsername());
        if (!mysql.isRunning()) {
            System.out.println("Container Logs:");
            System.out.println(mysql.getLogs());
        }
    }
}