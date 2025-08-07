-- ==========================================
-- 인덱스 최적화 스크립트 - 피드백 반영
-- ==========================================

-- 기존 비효율적인 인덱스 제거 (필요시)
-- DROP INDEX IF EXISTS idx_covering_all ON coupon_issues;

-- 1. 핵심 비즈니스 로직 기반 필수 인덱스만 선별

-- 사용자별 잔액 조회 (고빈도)
-- CREATE UNIQUE INDEX idx_user_balances_user_id ON user_balances(user_id); -- 이미 존재

-- 주문 번호로 주문 조회 (고빈도)
-- CREATE UNIQUE INDEX idx_orders_order_number ON orders(order_number); -- 이미 존재

-- 사용자별 쿠폰 중복 발급 방지 (동시성 제어)
-- CREATE UNIQUE INDEX idx_user_coupon_unique ON user_coupons(user_id, coupon_id); -- 이미 존재

-- 2. 실제 쿼리 패턴 기반 성능 인덱스

-- 사용자별 주문 목록 조회 (최신순) - 실제 사용되는 패턴
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);

-- 주문 항목 조회 (Repository 2번 호출 방식에서 사용)
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- 쿠폰 발급 가능 여부 확인 (실제 비즈니스 로직)
CREATE INDEX idx_coupons_availability ON coupons(expired_at, issued_quantity, total_quantity);

-- 3. 인기 상품 통계용 최적화 인덱스 (필요시에만)
-- 실제 통계 쿼리가 있을 때만 생성
CREATE INDEX idx_order_items_created_product ON order_items(created_at, product_id);

-- 4. 제거된 비효율적 인덱스들
-- idx_products_name - 상품 검색 빈도가 낮음
-- idx_products_price - 가격 검색 빈도가 낮음  
-- idx_balance_histories_user_created - 페이징 없이 최근 N개만 조회
-- idx_user_coupons_user_status - 사용자당 쿠폰 개수가 적음
-- 기타 커버링 인덱스 - 비효율적 저장공간 낭비

-- ==========================================
-- 인덱스 사용률 모니터링 쿼리
-- ==========================================

-- 생성된 인덱스 확인
SELECT 
    TABLE_NAME,
    INDEX_NAME, 
    COLUMN_NAME,
    SEQ_IN_INDEX,
    CARDINALITY,
    INDEX_TYPE
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'hhplus'
  AND TABLE_NAME IN ('orders', 'order_items', 'user_balances', 'user_coupons', 'coupons')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 테이블별 인덱스 크기 확인
SELECT 
    table_name,
    ROUND(((index_length) / 1024 / 1024), 2) AS index_size_mb,
    ROUND(((data_length) / 1024 / 1024), 2) AS data_size_mb,
    table_rows,
    ROUND(((index_length) / (data_length + index_length)) * 100, 2) AS index_ratio_percent
FROM information_schema.TABLES 
WHERE table_schema = 'hhplus'
  AND table_name IN ('orders', 'order_items', 'user_balances', 'user_coupons', 'coupons')
ORDER BY index_size_mb DESC;

-- ==========================================
-- 성능 테스트용 쿼리들
-- ==========================================

-- 1. 사용자별 주문 목록 조회 (인덱스 활용)
-- EXPLAIN SELECT * FROM orders WHERE user_id = 1 ORDER BY created_at DESC LIMIT 10;

-- 2. 주문 항목 조회 (Repository 2번 호출)
-- EXPLAIN SELECT * FROM order_items WHERE order_id = 1;

-- 3. 쿠폰 발급 가능 여부 확인
-- EXPLAIN SELECT * FROM coupons WHERE expired_at > NOW() AND issued_quantity < total_quantity;

-- 4. 사용자 쿠폰 중복 확인
-- EXPLAIN SELECT * FROM user_coupons WHERE user_id = 1 AND coupon_id = 1;