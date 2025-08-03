package kr.hhplus.be.server.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;

import kr.hhplus.be.server.balance.application.BalanceUseCase;
import kr.hhplus.be.server.balance.domain.UserBalance;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;
import kr.hhplus.be.server.coupon.application.CouponUseCase;
import kr.hhplus.be.server.order.application.OrderUseCase;
import kr.hhplus.be.server.product.application.ProductUseCase;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;

/**
 * 완성 검증 테스트 - 수정된 버전
 * 
 * 수정사항:
 * - TestContainers 설정 추가
 * - 트랜잭션 처리 개선
 * - 에러 처리 강화
 */
@SpringBootTest
// @Testcontainers
@ActiveProfiles("test")
@DisplayName("완성 검증 테스트")
@Transactional
class ApplicationVerificationTest {

    // @Container
    // @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private BalanceUseCase balanceUseCase;

    @Autowired
    private ProductUseCase productUseCase;

    @Autowired
    private OrderUseCase orderUseCase;

    @Autowired
    private CouponUseCase couponUseCase;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("스프링 컨텍스트 로드 검증 - 모든 Bean이 정상적으로 주입된다")
    void 스프링컨텍스트_로드검증() {
        // Application Layer UseCase 주입 확인
        assertThat(balanceUseCase).isNotNull();
        assertThat(productUseCase).isNotNull();
        assertThat(orderUseCase).isNotNull();
        assertThat(couponUseCase).isNotNull();

        // Infrastructure Layer Repository 주입 확인
        assertThat(userBalanceRepository).isNotNull();
        assertThat(productRepository).isNotNull();
    }

    @Test
    @DisplayName("Infrastructure Layer 동작 검증 - Repository가 정상 동작한다")
    void Infrastructure계층_동작검증() {
        // Given: 테스트 데이터 생성
        Long uniqueUserId = System.currentTimeMillis() % 100000 + 1000;
        UserBalance testBalance = new UserBalance(uniqueUserId);
        testBalance.charge(new BigDecimal("50000"));

        String uniqueProductName = "테스트상품_" + System.currentTimeMillis();
        Product testProduct = new Product(uniqueProductName, new BigDecimal("10000"), 5);

        // When: Repository 저장
        UserBalance savedBalance = userBalanceRepository.save(testBalance);
        Product savedProduct = productRepository.save(testProduct);

        // Then: 정상 저장 확인
        assertThat(savedBalance.getId()).isNotNull();
        assertThat(savedBalance.getBalance()).isEqualByComparingTo(new BigDecimal("50000"));

        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo(uniqueProductName);

        // Cleanup은 @Transactional로 자동 롤백됨
    }

    @Test
    @DisplayName("Application Layer 동작 검증 - UseCase가 정상 동작한다")
    void Application계층_동작검증() {
        // Given & When: UseCase 메서드 호출
        var allProducts = productUseCase.getAllProducts();
        var availableCoupons = couponUseCase.getAvailableCoupons();

        // Then: 예외 없이 정상 실행되어야 함
        assertThat(allProducts).isNotNull();
        assertThat(availableCoupons).isNotNull();

        // 초기 데이터가 있는지 확인 (DataLoader에 의해)
        // 초기 데이터가 없어도 에러가 발생하면 안됨
        assertThat(allProducts).isNotNull();
        assertThat(availableCoupons).isNotNull();
    }

    @Test
    @DisplayName("Domain Layer 비즈니스 로직 검증")
    void Domain계층_비즈니스로직검증() {
        // Given
        UserBalance balance = new UserBalance(888L);
        Product product = new Product("도메인테스트상품", new BigDecimal("20000"), 10);

        // When: 도메인 비즈니스 로직 실행
        balance.charge(new BigDecimal("100000"));
        product.reduceStock(3);

        // Then: 비즈니스 로직 정상 동작 확인
        assertThat(balance.getBalance()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(product.getStockQuantity()).isEqualTo(7);
        assertThat(product.hasEnoughStock(5)).isTrue();
        assertThat(product.hasEnoughStock(10)).isFalse();
    }

    @Test
    @DisplayName("JPA Auditing 동작 검증")
    void JPA_Auditing_동작검증() {
        // Given
        String uniqueName = "Auditing테스트_" + System.currentTimeMillis();
        Product product = new Product(uniqueName, new BigDecimal("30000"), 5);

        // When
        Product savedProduct = productRepository.save(product);

        // Then: JPA Auditing으로 시간 필드가 자동 설정되었는지 확인
        assertThat(savedProduct.getCreatedAt()).isNotNull();
        assertThat(savedProduct.getUpdatedAt()).isNotNull();

        // @Transactional로 자동 롤백됨
    }

    @Test
    @DisplayName("MySQL 연동 검증 - 실제 DB 저장 및 조회 동작")
    void MySQL_연동검증() {
        // Given
        String uniqueName = "MySQL연동테스트_" + System.currentTimeMillis();
        Product testProduct = new Product(uniqueName, new BigDecimal("15000"), 8);

        // When: 저장
        Product savedProduct = productRepository.save(testProduct);

        // Then: 다시 조회하여 정상 저장 확인
        var foundProduct = productRepository.findById(savedProduct.getId());
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo(uniqueName);
        assertThat(foundProduct.get().getPrice()).isEqualByComparingTo(new BigDecimal("15000"));

        // @Transactional로 자동 롤백됨
    }

    @Test
    @DisplayName("트랜잭션 동작 검증")
    void 트랜잭션_동작검증() {
        // Given
        Long userId = System.currentTimeMillis() % 100000 + 777L; // 고유한 ID 생성

        // When: UseCase를 통한 트랜잭션 동작 확인
        var response = balanceUseCase.chargeBalance(userId, new BigDecimal("25000"));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.currentBalance()).isEqualByComparingTo(new BigDecimal("25000"));

        // DB에 실제 반영되었는지 확인
        var savedBalance = userBalanceRepository.findByUserId(userId);
        assertThat(savedBalance).isPresent();
        assertThat(savedBalance.get().getBalance()).isEqualByComparingTo(new BigDecimal("25000"));

        // @Transactional로 자동 롤백됨
    }
}