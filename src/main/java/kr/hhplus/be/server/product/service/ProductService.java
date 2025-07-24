package kr.hhplus.be.server.product.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.exception.ErrorCode;
import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.dto.ProductResponse;
import kr.hhplus.be.server.product.exception.ProductNotFoundException;
import kr.hhplus.be.server.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 서비스
 * 
 * ✨ 설계 원칙:
 * - 단일 책임: 상품 관련 비즈니스 로직만 처리
 * - 의존성 역전: ProductRepository 인터페이스에만 의존
 * - 트랜잭션 관리: @Transactional로 데이터 일관성 보장
 * 
 * 🎯 책임:
 * - 상품 조회 및 검색
 * - 재고 관리 (차감, 복구, 확인)
 * - 비즈니스 규칙 검증
 * - DTO 변환
 */
@Slf4j // 로깅을 위한 Lombok 어노테이션
@Service
@Transactional(readOnly = true) // 기본적으로 읽기 전용 트랜잭션
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 생성자 주입 (스프링 권장 방식)
     * 
     * 🎯 장점:
     * - final 키워드로 불변성 보장
     * - 테스트 시 Mock 객체 주입 용이
     * - 순환 의존성 컴파일 타임 감지
     */
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 모든 상품 조회
     * 
     * @return 전체 상품 목록 (DTO 변환됨)
     */
    public List<ProductResponse> getAllProducts() {
        log.debug("📋 전체 상품 목록 조회 요청");

        List<Product> products = productRepository.findAll();

        log.debug("✅ 총 {}개 상품 조회 완료", products.size());

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * 특정 상품 조회
     * 
     * @param productId 조회할 상품 ID
     * @return 상품 정보 DTO
     * @throws ProductNotFoundException 상품을 찾을 수 없는 경우
     */
    public ProductResponse getProduct(Long productId) {
        log.debug("🔍 상품 조회 요청: ID = {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("❌ 상품을 찾을 수 없음: ID = {}", productId);
                    return new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND);
                });

        log.debug("✅ 상품 조회 성공: {}", product.getName());

        return convertToResponse(product);
    }

    /**
     * 상품명으로 검색
     * 
     * @param name 검색할 상품명 (부분 일치)
     * @return 검색된 상품 목록
     */
    public List<ProductResponse> searchProductsByName(String name) {
        log.debug("🔍 상품명 검색 요청: '{}'", name);

        if (name == null || name.trim().isEmpty()) {
            return getAllProducts();
        }

        List<Product> products = productRepository.findByNameContaining(name.trim());

        log.debug("✅ 상품명 검색 완료: '{}' - {}개 결과", name, products.size());

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * 가격 범위로 상품 검색
     * 
     * @param minPrice 최소 가격
     * @param maxPrice 최대 가격
     * @return 가격 범위에 해당하는 상품 목록
     */
    public List<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("💰 가격 범위 검색 요청: {} ~ {}", minPrice, maxPrice);

        List<Product> products = productRepository.findByPriceBetween(minPrice, maxPrice);

        log.debug("✅ 가격 범위 검색 완료: {}개 결과", products.size());

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * 재고가 있는 상품만 조회
     * 
     * @return 재고가 있는 상품 목록
     */
    public List<ProductResponse> getAvailableProducts() {
        log.debug("📦 재고 있는 상품 조회 요청");

        List<Product> products = productRepository.findByStockQuantityGreaterThan(0);

        log.debug("✅ 재고 있는 상품 조회 완료: {}개", products.size());

        return products.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * 재고 확인
     * 
     * @param productId 상품 ID
     * @param quantity  필요한 수량
     * @return 재고 충분 여부
     */
    public boolean hasEnoughStock(Long productId, int quantity) {
        log.debug("📊 재고 확인 요청: 상품 ID = {}, 필요 수량 = {}", productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        boolean hasEnough = product.hasEnoughStock(quantity);

        log.debug("✅ 재고 확인 결과: {} (현재 재고: {})",
                hasEnough ? "충분" : "부족", product.getStockQuantity());

        return hasEnough;
    }

    /**
     * 재고 차감 (주문 시 호출)
     * 
     * 🔒 동시성 고려사항:
     * - findByIdForUpdate로 비관적 락 적용
     * - 여러 사용자가 동시에 주문해도 정확한 재고 차감 보장
     * 
     * @param productId 상품 ID
     * @param quantity  차감할 수량
     */
    @Transactional // 쓰기 작업이므로 readOnly=false
    public void reduceStock(Long productId, int quantity) {
        log.debug("📉 재고 차감 요청: 상품 ID = {}, 차감 수량 = {}", productId, quantity);

        // 비관적 락으로 상품 조회 (동시성 제어)
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> {
                    log.error("❌ 재고 차감 실패 - 상품 없음: ID = {}", productId);
                    return new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND);
                });

        int beforeStock = product.getStockQuantity();

        // 도메인 객체의 비즈니스 로직 호출
        product.reduceStock(quantity);

        // 변경된 상품 저장
        productRepository.save(product);

        log.info("✅ 재고 차감 완료: 상품 '{}', {} → {} (차감: {})",
                product.getName(), beforeStock, product.getStockQuantity(), quantity);
    }

    /**
     * 재고 복구 (주문 취소, 결제 실패 시)
     * 
     * @param productId 상품 ID
     * @param quantity  복구할 수량
     */
    @Transactional
    public void restoreStock(Long productId, int quantity) {
        log.debug("📈 재고 복구 요청: 상품 ID = {}, 복구 수량 = {}", productId, quantity);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("❌ 재고 복구 실패 - 상품 없음: ID = {}", productId);
                    return new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND);
                });

        int beforeStock = product.getStockQuantity();

        // 도메인 객체의 재고 복구 로직 호출
        product.restoreStock(quantity);

        // 변경된 상품 저장
        productRepository.save(product);

        log.info("✅ 재고 복구 완료: 상품 '{}', {} → {} (복구: {})",
                product.getName(), beforeStock, product.getStockQuantity(), quantity);
    }

    /**
     * 상품 생성 (관리자 기능)
     * 
     * @param name          상품명
     * @param price         가격
     * @param stockQuantity 초기 재고
     * @return 생성된 상품 정보
     */
    @Transactional
    public ProductResponse createProduct(String name, BigDecimal price, Integer stockQuantity) {
        log.debug("🆕 상품 생성 요청: '{}', 가격 {}, 재고 {}", name, price, stockQuantity);

        Product product = new Product(name, price, stockQuantity);
        Product savedProduct = productRepository.save(product);

        log.info("✅ 상품 생성 완료: ID = {}, 이름 = '{}'", savedProduct.getId(), savedProduct.getName());

        return convertToResponse(savedProduct);
    }

    /**
     * 상품 정보 수정 (관리자 기능)
     * 
     * @param productId 수정할 상품 ID
     * @param name      새 상품명
     * @param price     새 가격
     * @return 수정된 상품 정보
     */
    @Transactional
    public ProductResponse updateProduct(Long productId, String name, BigDecimal price) {
        log.debug("✏️ 상품 수정 요청: ID = {}, 이름 = '{}', 가격 = {}", productId, name, price);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        String oldName = product.getName();
        BigDecimal oldPrice = product.getPrice();

        product.updateProductInfo(name, price);
        Product savedProduct = productRepository.save(product);

        log.info("✅ 상품 수정 완료: '{}' → '{}', {} → {}",
                oldName, name, oldPrice, price);

        return convertToResponse(savedProduct);
    }

    /**
     * 상품 삭제 (관리자 기능)
     * 
     * @param productId 삭제할 상품 ID
     */
    @Transactional
    public void deleteProduct(Long productId) {
        log.debug("🗑️ 상품 삭제 요청: ID = {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(ErrorCode.PRODUCT_NOT_FOUND));

        String productName = product.getName();
        productRepository.delete(product);

        log.info("✅ 상품 삭제 완료: ID = {}, 이름 = '{}'", productId, productName);
    }

    /**
     * Domain 객체를 DTO로 변환
     * 
     * 🎯 DTO 변환 이유:
     * - 외부에 도메인 객체 노출 방지
     * - API 스펙 안정성 확보
     * - 필요한 정보만 선별적 노출
     */
    private ProductResponse convertToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCreatedAt());
    }
}