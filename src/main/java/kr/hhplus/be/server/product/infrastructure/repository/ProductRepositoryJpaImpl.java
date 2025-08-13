package kr.hhplus.be.server.product.infrastructure.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class ProductRepositoryJpaImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Product save(Product product) {
        return jpaRepository.save(product);
    }

    @Override
    public void delete(Product product) {
        jpaRepository.delete(product);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
        log.debug("🗑️ 상품 삭제: id = {}", id);
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long id) {
        log.debug("🔒 상품 비관적 락 조회: id = {}", id);

        // ✅ 현업 방식: SELECT FOR UPDATE로 재고 차감 시 동시성 제어
        return jpaRepository.findByIdForUpdate(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByNameContaining(String name) {
        return jpaRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return jpaRepository.findByPriceBetween(minPrice, maxPrice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByStockQuantityGreaterThan(Integer quantity) {
        return jpaRepository.findByStockQuantityGreaterThan(quantity);
    }
}