package kr.hhplus.be.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;

/**
 * 최소한의 TestContainers 검증 테스트
 * 
 * 이 테스트는 가장 기본적인 기능만 확인하여
 * TestContainers 설정이 올바른지 검증합니다.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("최소한의 TestContainers 검증")
public class MinimalTestContainersTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false);

    @Autowired
    private ProductRepository productRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.sql.init.mode", () -> "never");

        System.out.println("✅ MySQL Container: " + mysql.getJdbcUrl());
    }

    @Test
    @DisplayName("TestContainers 기본 동작 확인")
    @Transactional
    void testContainers_기본동작() {
        // 1. 컨테이너 실행 확인
        assertThat(mysql.isRunning()).isTrue();
        System.out.println("✅ MySQL 컨테이너 실행 중");

        // 2. Repository 주입 확인
        assertThat(productRepository).isNotNull();
        System.out.println("✅ ProductRepository 주입 성공");

        // 3. 기본 저장 테스트
        Product product = new Product("테스트상품", new BigDecimal("1000"), 1);
        Product saved = productRepository.save(product);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("테스트상품");
        System.out.println("✅ 상품 저장 성공: " + saved.getId());

        // 4. 기본 조회 테스트
        var found = productRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("테스트상품");
        System.out.println("✅ 상품 조회 성공: " + found.get().getName());

        // 5. 기본 목록 조회 테스트
        var products = productRepository.findAll();
        assertThat(products).isNotEmpty();
        System.out.println("✅ 상품 목록 조회 성공: " + products.size() + "개");

        System.out.println("🎉 TestContainers 기본 검증 완료!");
    }

    @Test
    @DisplayName("컨텍스트 로드 확인")
    void contextLoads() {
        // Spring 컨텍스트가 정상적으로 로드되었는지 확인
        assertThat(productRepository).isNotNull();
        System.out.println("✅ Spring 컨텍스트 로드 성공");
    }

    @Test
    @DisplayName("MySQL 연결 확인")
    void mysqlConnection() {
        // MySQL 컨테이너 연결 상태 확인
        assertThat(mysql.isRunning()).isTrue();
        assertThat(mysql.getJdbcUrl()).contains("testdb");

        System.out.println("📦 Container ID: " + mysql.getContainerId());
        System.out.println("🔗 JDBC URL: " + mysql.getJdbcUrl());
        System.out.println("👤 Username: " + mysql.getUsername());
        System.out.println("✅ MySQL 연결 정보 확인 완료");
    }
}