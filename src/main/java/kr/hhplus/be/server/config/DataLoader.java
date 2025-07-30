package kr.hhplus.be.server.config;

import java.math.BigDecimal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.product.domain.Product;
import kr.hhplus.be.server.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA 환경에서의 초기 데이터 로딩
 * 
 * @PostConstruct 대신 ApplicationRunner 사용:
 *                - 트랜잭션 컨텍스트에서 안전하게 실행
 *                - 모든 Bean 초기화 완료 후 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("🏪 테스트 데이터 초기화 시작...");

        // 기존 데이터가 있는지 확인
        if (productRepository.findAll().isEmpty()) {
            initializeProducts();
            log.info("✅ 상품 테스트 데이터 초기화 완료!");
        } else {
            log.info("📦 기존 상품 데이터 존재, 초기화 생략");
        }
    }

    private void initializeProducts() {
        productRepository.save(new Product("고성능 노트북", new BigDecimal("1500000"), 10));
        productRepository.save(new Product("무선 마우스", new BigDecimal("50000"), 50));
        productRepository.save(new Product("기계식 키보드", new BigDecimal("150000"), 25));
        productRepository.save(new Product("27인치 모니터", new BigDecimal("300000"), 15));
        productRepository.save(new Product("HD 웹캠", new BigDecimal("80000"), 30));
        productRepository.save(new Product("게이밍 의자", new BigDecimal("400000"), 8));

        log.info("💾 6개 상품 초기 데이터 저장 완료");
    }
}