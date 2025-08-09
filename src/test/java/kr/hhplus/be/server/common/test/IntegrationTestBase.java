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
 * 통합 테스트 기본 클래스 - 개선된 버전
 * 
 * 개선사항:
 * - 더 상세한 디버깅 정보 제공
 * - TestContainers 안정성 향상
 * - 에러 진단 기능 강화
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.jpa.defer-datasource-initialization=false",
        "logging.level.org.testcontainers=INFO",
        "logging.level.org.springframework.web=DEBUG", // 웹 관련 디버깅 활성화
        "logging.level.org.springframework.boot.web=DEBUG" // 부트 웹 디버깅 활성화
})
public abstract class IntegrationTestBase {

    @Container
    protected static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withCommand(
                    "--default-authentication-plugin=mysql_native_password",
                    "--character-set-server=utf8mb4",
                    "--collation-server=utf8mb4_unicode_ci",
                    "--skip-ssl",
                    "--disable-log-bin")
            .withStartupTimeout(Duration.ofMinutes(5)) // 타임아웃 증가
            .withConnectTimeoutSeconds(300)
            .withEnv("MYSQL_ROOT_PASSWORD", "root")
            .withEnv("MYSQL_ROOT_HOST", "%")
            .withReuse(true); // 재사용 활성화로 속도 향상

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected EntityManager entityManager;

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

        // HikariCP 연결 풀 안정성 설정 - 테스트 환경에 최적화
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "3");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "1");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "60000"); // 60초로 증가
        registry.add("spring.datasource.hikari.idle-timeout", () -> "600000");
        registry.add("spring.datasource.hikari.max-lifetime", () -> "1200000");
        registry.add("spring.datasource.hikari.validation-timeout", () -> "30000");
        registry.add("spring.datasource.hikari.initialization-fail-timeout", () -> "60000");
        registry.add("spring.datasource.hikari.connection-test-query", () -> "SELECT 1");

        // JPA/Hibernate 설정
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false"); // 성능을 위해 false
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.hbm2ddl.auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never"); // 중복 초기화 방지

        // 개선된 로깅
        System.out.println("🔧 TestContainers Configuration Applied:");
        System.out.println("   JDBC URL: " + mysql.getJdbcUrl());
        System.out.println("   Database: " + mysql.getDatabaseName());
        System.out.println("   Username: " + mysql.getUsername());
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("🚀 Starting Integration Test Environment...");

        try {
            if (!mysql.isRunning()) {
                System.out.println("🔄 Starting MySQL container...");
                mysql.start();
                
                // 컨테이너 완전 시작 대기
                Thread.sleep(5000);
                
                // 연결 테스트
                waitForDatabaseReady();
            }

            if (!mysql.isRunning()) {
                throw new IllegalStateException("❌ MySQL 컨테이너 시작 실패!");
            }

            System.out.println("✅ MySQL Container Ready:");
            System.out.println("   🔗 JDBC URL: " + mysql.getJdbcUrl());
            System.out.println("   📦 Container ID: " + mysql.getContainerId());
            System.out.println("   🚀 Startup Time: " + mysql.getStartupAttempts() + " attempts");
            
        } catch (Exception e) {
            System.err.println("❌ MySQL 컨테이너 초기화 실패: " + e.getMessage());
            throw new RuntimeException("TestContainer 초기화 실패", e);
        }
    }
    
    /**
     * 데이터베이스 준비 상태 대기
     */
    private static void waitForDatabaseReady() {
        System.out.println("🔍 Waiting for database to be ready...");
        
        int maxAttempts = 30;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            try {
                attempt++;
                
                // 간단한 연결 테스트
                String jdbcUrl = mysql.getJdbcUrl();
                System.out.println("🔗 Testing connection attempt " + attempt + ": " + jdbcUrl);
                
                // 컨테이너가 완전히 준비될 때까지 대기
                if (mysql.isRunning()) {
                    Thread.sleep(2000); // 2초 추가 대기
                    System.out.println("✅ Database ready!");
                    return;
                }
                
                Thread.sleep(2000);
                
            } catch (Exception e) {
                System.out.println("⏳ Database not ready yet, attempt " + attempt + "/" + maxAttempts);
                if (attempt >= maxAttempts) {
                    throw new RuntimeException("Database failed to be ready after " + maxAttempts + " attempts", e);
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for database", ie);
                }
            }
        }
    }

    @BeforeEach
    void setUp() {
        verifyTestEnvironment();
    }

    /**
     * 테스트 환경 검증 - 개선된 버전
     */
    protected void verifyTestEnvironment() {
        if (!mysql.isRunning()) {
            throw new IllegalStateException("❌ MySQL 컨테이너가 실행되지 않았습니다.");
        }

        // 추가 검증
        try {
            // RestTemplate이 정상 주입되었는지 확인
            if (restTemplate == null) {
                throw new IllegalStateException("❌ TestRestTemplate이 주입되지 않았습니다.");
            }

            // EntityManager 확인
            if (entityManager == null) {
                throw new IllegalStateException("❌ EntityManager가 주입되지 않았습니다.");
            }

            System.out.println("✅ Test Environment Verified");

        } catch (Exception e) {
            System.err.println("❌ Test Environment Verification Failed: " + e.getMessage());
            logContainerStatus();
            throw e;
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
        try {
            entityManager.flush();
            entityManager.clear();
            System.out.println("💾 EntityManager flush & clear completed");
        } catch (Exception e) {
            System.err.println("❌ EntityManager flush & clear failed: " + e.getMessage());
        }
    }

    /**
     * 컨테이너 상태 로깅 - 개선된 버전
     */
    protected void logContainerStatus() {
        System.out.println("=== TestContainers Status ===");
        System.out.println("🔍 Running: " + mysql.isRunning());
        System.out.println("🔍 Container ID: " + mysql.getContainerId());

        try {
            System.out.println("🔍 JDBC URL: " + mysql.getJdbcUrl());
            System.out.println("🔍 Mapped Port: " + mysql.getMappedPort(3306));
            System.out.println("🔍 Host Port: " + mysql.getHost() + ":" + mysql.getFirstMappedPort());

            // 추가 진단 정보
            System.out.println("🔍 Database Name: " + mysql.getDatabaseName());
            System.out.println("🔍 Username: " + mysql.getUsername());
            System.out.println("🔍 Container State: " + mysql.getContainerInfo().getState().getStatus());

        } catch (Exception e) {
            System.err.println("❌ Status Error: " + e.getMessage());
        }

        if (!mysql.isRunning()) {
            try {
                System.out.println("=== Container Logs ===");
                System.out.println(mysql.getLogs());
            } catch (Exception e) {
                System.err.println("❌ 로그 조회 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 테스트 실패 시 디버깅 - 개선된 버전
     */
    protected void debugTestFailure(String testName, Exception e) {
        System.err.println("❌ Test Failed: " + testName);
        System.err.println("📍 Error: " + e.getMessage());
        System.err.println("📍 Error Type: " + e.getClass().getSimpleName());

        logContainerStatus();

        // 스택 트레이스의 핵심 부분만 출력
        if (e.getCause() != null) {
            System.err.println("📍 Root Cause: " + e.getCause().getMessage());
        }

        // RestTemplate 상태 확인
        if (restTemplate != null) {
            System.out.println("🔍 RestTemplate URI: " + restTemplate.getRootUri());
        } else {
            System.err.println("❌ RestTemplate is null");
        }

        // Spring Boot 애플리케이션 상태 확인
        try {
            var healthResponse = restTemplate.getForEntity("/actuator/health", String.class);
            System.out.println("🔍 App Health Status: " + healthResponse.getStatusCode());
        } catch (Exception healthEx) {
            System.err.println("❌ App Health Check Failed: " + healthEx.getMessage());
        }
    }

    /**
     * API 엔드포인트 존재 여부 확인
     */
    protected void checkApiEndpoints() {
        System.out.println("=== API Endpoints Check ===");

        String[] endpoints = {
                "/actuator/health",
                "/api/v1/products",
                "/api/v1/coupons/available",
                "/api/v1/orders"
        };

        for (String endpoint : endpoints) {
            try {
                var response = restTemplate.getForEntity(endpoint, String.class);
                System.out.println("✅ " + endpoint + " -> " + response.getStatusCode());
            } catch (Exception e) {
                System.err.println("❌ " + endpoint + " -> " + e.getMessage());
            }
        }
    }
}