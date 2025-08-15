package kr.hhplus.be.server.product.repository;

import java.util.List;
import java.util.Optional;

import kr.hhplus.be.server.product.domain.Product;

/**
 * 상품 저장소 인터페이스
 * 
 * ✨ 설계 원칙:
 * - DIP(의존성 역전 원칙) 적용: Service는 이 인터페이스에만 의존
 * - 구현 기술(JPA, MongoDB 등)로부터 비즈니스 로직 격리
 * - 테스트 시 Mock 객체로 쉽게 대체 가능
 * 
 * 🎯 책임:
 * - 상품 CRUD 기본 연산
 * - 동시성 제어를 위한 락 기능 (STEP06에서 활용)
 */
public interface ProductRepository {

    /**
     * ID로 상품 조회
     * 
     * @param id 상품 ID
     * @return 상품 정보 (Optional로 null 안전성 보장)
     */
    Optional<Product> findById(Long id);

    /**
     * 모든 상품 목록 조회
     * 
     * @return 전체 상품 목록
     */
    List<Product> findAll();

    /**
     * 상품 저장 (생성 또는 수정)
     * 
     * @param product 저장할 상품
     * @return 저장된 상품 (ID가 할당된 상태)
     */
    Product save(Product product);

    /**
     * 상품 삭제
     * 
     * @param product 삭제할 상품
     */
    void delete(Product product);

    /**
     * ID로 상품 삭제
     * 
     * @param id 삭제할 상품 ID
     */
    void deleteById(Long id);

    // 비관적 락 메서드 제거 - 분산락으로 대체

    /**
     * 상품명으로 검색
     * 
     * @param name 검색할 상품명 (부분 일치)
     * @return 일치하는 상품 목록
     */
    List<Product> findByNameContaining(String name);

    /**
     * 가격 범위로 상품 검색
     * 
     * @param minPrice 최소 가격
     * @param maxPrice 최대 가격
     * @return 가격 범위에 해당하는 상품 목록
     */
    List<Product> findByPriceBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    /**
     * 재고가 있는 상품만 조회
     * 
     * @return 재고가 0보다 큰 상품 목록
     */
    List<Product> findByStockQuantityGreaterThan(Integer quantity);
}