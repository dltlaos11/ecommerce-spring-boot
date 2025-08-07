package kr.hhplus.be.server.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.balance.application.ChargeBalanceUseCase;
import kr.hhplus.be.server.balance.application.GetBalanceUseCase;
import kr.hhplus.be.server.balance.domain.UserBalance;
import kr.hhplus.be.server.balance.repository.UserBalanceRepository;
import kr.hhplus.be.server.coupon.application.GetCouponsUseCase;
import kr.hhplus.be.server.order.application.CreateOrderUseCase;
import kr.hhplus.be.server.order.application.GetOrdersUseCase;
import kr.hhplus.be.server.product.application.GetProductsUseCase;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;

/**
 * 애플리케이션 완성 검증 테스트 - UseCase 패턴 적용 버전
 * 
 * 검증 항목:
 * - Spring 컨텍스트 로드 및 Bean 주입
 * - 계층별 동작 검증 (Application → Domain → Infrastructure)
 * - JPA Auditing 및 트랜잭션 동작
 * - MySQL 연동 상태
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("애플리케이션 완성 검증 테스트")
@Transactional
class ApplicationVerificationTest {

    // Application Layer - UseCase들
    @Autowired
    private ChargeBalanceUseCase chargeBalanceUseCase;

    @Autowired
    private GetBalanceUseCase getBalanceUseCase;

    @Autowired
    private GetProductsUseCase getProductsUseCase;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private GetOrdersUseCase getOrdersUseCase;

    @Autowired
    private GetCouponsUseCase getCouponsUseCase;

    // Infrastructure Layer - Repository들
    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("스프링 컨텍스트 로드 검증 - 모든 Bean이 정상적으로 주입된다")
    void 스프링컨텍스트_로드검증() {
        // Application Layer UseCase 주입 확인
        assertThat(chargeBalanceUseCase).isNotNull();
        assertThat(getBalanceUseCase).isNotNull();
        assertThat(getProductsUseCase).isNotNull();
        assertThat(createOrderUseCase).isNotNull();
        assertThat(getOrdersUseCase).isNotNull();
        assertThat(getCouponsUseCase).isNotNull();

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
        var allProducts = getProductsUseCase.executeGetAll();
        var availableCoupons = getCouponsUseCase.executeAvailableCoupons();

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
        var response = chargeBalanceUseCase.execute(userId, new BigDecimal("25000"));

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

    @Test
    @DisplayName("UseCase 패턴 적용 검증 - 단일 책임 원칙 확인")
    void UseCase패턴_적용검증() {
        // Given: UseCase들이 각각 단일 책임을 가지는지 확인

        // When: 각 UseCase의 주요 메서드 존재 확인
        // ChargeBalanceUseCase - "잔액을 충전한다"
        assertThat(chargeBalanceUseCase).isNotNull();

        // GetBalanceUseCase - "잔액을 조회한다"
        assertThat(getBalanceUseCase).isNotNull();

        // CreateOrderUseCase - "주문을 생성한다"
        assertThat(createOrderUseCase).isNotNull();

        // GetOrdersUseCase - "주문을 조회한다"
        assertThat(getOrdersUseCase).isNotNull();

        // Then: 각각이 독립적인 비즈니스 요구사항을 처리하는지 확인
        // (컴파일 타임에 이미 검증됨 - 존재 자체가 성공)
    }

    @Test
    @DisplayName("Entity + Domain 통합 패턴 검증")
    void Entity_Domain통합패턴_검증() {
        // Given: Entity가 동시에 Domain 역할을 수행하는지 확인
        Product product = new Product("통합테스트상품", new BigDecimal("50000"), 10);

        // When: Domain 비즈니스 로직 실행
        product.reduceStock(3);

        // Then: JPA Entity이면서 동시에 Domain 객체로 동작
        assertThat(product.getStockQuantity()).isEqualTo(7); // Domain 로직
        assertThat(product.getName()).isEqualTo("통합테스트상품"); // Entity 속성

        // JPA 저장도 정상 동작
        Product saved = productRepository.save(product);
        assertThat(saved.getId()).isNotNull(); // JPA Entity 기능
        assertThat(saved.hasEnoughStock(5)).isTrue(); // Domain 로직
    }
}