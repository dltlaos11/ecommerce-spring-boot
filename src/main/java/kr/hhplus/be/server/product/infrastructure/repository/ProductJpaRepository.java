package kr.hhplus.be.server.product.infrastructure.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.product.domain.Product; // ✅ 통합된 Entity+Domain

/**
 * ✅ Entity-Domain 통합 버전 JPA Repository
 */
public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    /**
     * 비관적 락으로 상품 조회 (재고 차감용)
     * 🔒 SELECT FOR UPDATE - 동시 재고 차감 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    /**
     * 상품명 검색 (부분 일치)
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * 가격 범위 검색
     */
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * 재고가 있는 상품만 조회
     */
    List<Product> findByStockQuantityGreaterThan(Integer quantity);
}