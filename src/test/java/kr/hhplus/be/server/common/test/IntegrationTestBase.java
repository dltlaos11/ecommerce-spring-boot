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

/**
 * 통합 테스트 기본 클래스 - 데이터 격리 포함
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/test-data-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class IntegrationTestBase {

    // ✅ 싱글톤 컨테이너 - 모든 테스트에서 공유
    private static final MySQLContainer<?> mysql;

    static {
        mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withCommand("--default-authentication-plugin=mysql_native_password")
                .withStartupTimeout(Duration.ofMinutes(3))
                .withReuse(true);

        mysql.start();
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    // ✅ Spring에 DB 설정 주입
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // 테스트 환경 최적화
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("logging.level.org.hibernate.SQL", () -> "DEBUG");
    }

    @BeforeAll
    static void beforeAll() {
        if (!mysql.isRunning()) {
            throw new IllegalStateException("MySQL 컨테이너가 시작되지 않았습니다.");
        }
        System.out.println("✅ MySQL Container started: " + mysql.getJdbcUrl());
    }

    @BeforeEach
    void setUp() {
        verifyTestEnvironment();
    }

    protected void verifyTestEnvironment() {
        if (!mysql.isRunning()) {
            throw new IllegalStateException("MySQL 컨테이너가 실행되지 않았습니다.");
        }
    }

    protected Long generateUniqueUserId() {
        return System.currentTimeMillis() % 100000 + 1000;
    }

    protected String generateUniqueProductName(String baseName) {
        return baseName + "_" + System.currentTimeMillis();
    }
}