package kr.hhplus.be.server.product.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import kr.hhplus.be.server.product.domain.Product;

/**
 * 인메모리 상품 저장소 구현체
 * 
 * ✨ 기술적 특징:
 * - ConcurrentHashMap: 스레드 안전한 해시맵
 * - AtomicLong: 원자적 ID 생성
 * - ReentrantLock: 비관적 락 구현
 * 
 * 🎯 실제 서비스라면:
 * - JPA Repository 구현체 사용
 * - 데이터베이스 연동
 * - 쿼리 최적화 등
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    // 💾 인메모리 데이터 저장소
    private final Map<Long, Product> products = new ConcurrentHashMap<>();

    // 🔢 ID 자동 생성기
    private final AtomicLong idGenerator = new AtomicLong(1);

    // 🔒 상품별 락 관리 (비관적 락용)
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public List<Product> findAll() {
        // 원본 데이터 보호를 위해 새로운 리스트로 복사
        return new ArrayList<>(products.values());
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            // 새 상품: ID 자동 생성
            Long newId = idGenerator.getAndIncrement();
            product.setId(newId);

            // 락도 함께 생성
            locks.put(newId, new ReentrantLock());
        }

        // 저장 시간 갱신
        product.setUpdatedAt(LocalDateTime.now());

        // 저장
        products.put(product.getId(), product);

        return product;
    }

    @Override
    public void delete(Product product) {
        if (product.getId() != null) {
            products.remove(product.getId());
            locks.remove(product.getId());
        }
    }

    @Override
    public void deleteById(Long id) {
        products.remove(id);
        locks.remove(id);
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long id) {
        /*
         * 🔒 비관적 락 구현
         * 
         * 실제 동작:
         * 1. 해당 상품의 락 객체 획득
         * 2. 락 보유 중인 동안은 다른 스레드 대기
         * 3. 비즈니스 로직 완료 후 트랜잭션 종료 시 락 해제
         * 
         * 주의: 실제 구현에서는 try-finally로 락 해제 보장 필요
         * 여기서는 Service의 @Transactional이 끝날 때 자동 해제된다고 가정
         */
        ReentrantLock lock = locks.get(id);
        if (lock != null) {
            lock.lock(); // 🔒 락 획득
            // 실제로는 트랜잭션 매니저가 락을 관리해야 함
        }

        return findById(id);
    }

    @Override
    public List<Product> findByNameContaining(String name) {
        return products.values().stream()
                .filter(product -> product.getName().toLowerCase()
                        .contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return products.values().stream()
                .filter(product -> product.getPrice().compareTo(minPrice) >= 0 &&
                        product.getPrice().compareTo(maxPrice) <= 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByStockQuantityGreaterThan(Integer quantity) {
        return products.values().stream()
                .filter(product -> product.getStockQuantity() > quantity)
                .collect(Collectors.toList());
    }

    /**
     * 애플리케이션 시작 시 테스트 데이터 자동 생성
     * 
     * 🎯 실제 서비스라면:
     * - 데이터베이스에서 읽어옴
     * - 또는 별도의 데이터 마이그레이션 스크립트 사용
     */
    @PostConstruct
    public void initData() {
        System.out.println("🏪 상품 테스트 데이터 초기화 시작...");

        save(new Product("고성능 노트북", new BigDecimal("1500000"), 10));
        save(new Product("무선 마우스", new BigDecimal("50000"), 50));
        save(new Product("기계식 키보드", new BigDecimal("150000"), 25));
        save(new Product("27인치 모니터", new BigDecimal("300000"), 15));
        save(new Product("HD 웹캠", new BigDecimal("80000"), 30));
        save(new Product("게이밍 의자", new BigDecimal("400000"), 8));

        System.out.println("✅ 상품 테스트 데이터 초기화 완료! 총 " + products.size() + "개 상품");
    }

    /**
     * 테스트 및 개발용: 모든 데이터 초기화
     */
    public void clear() {
        products.clear();
        locks.clear();
        idGenerator.set(1);
        System.out.println("🗑️ 상품 데이터 모두 삭제됨");
    }

    /**
     * 현재 저장된 상품 수 반환 (디버깅용)
     */
    public int count() {
        return products.size();
    }
}