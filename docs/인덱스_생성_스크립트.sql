-- ==========================================
-- STEP08 성능 최적화 인덱스 생성 스크립트
-- ==========================================

-- 1. 인기 상품 통계 최적화 인덱스
-- 복합 인덱스: 날짜 + 상품ID (WHERE + GROUP BY 최적화)
CREATE INDEX idx_order_items_created_product
ON order_items(created_at, product_id);

-- 커버링 인덱스: SELECT 절 모든 컬럼 포함 (테이블 접근 제거)
CREATE INDEX idx_order_items_stats_covering
ON order_items(created_at, product_id, quantity, subtotal, product_name, product_price);

-- 2. 사용자 주문 목록 최적화
-- 복합 인덱스: 사용자 + 생성일 정렬 (WHERE + ORDER BY 최적화)
CREATE INDEX idx_orders_user_created
ON orders(user_id, created_at DESC);

-- 3. 잔액 이력 최적화
-- 복합 인덱스: 사용자 + 시간순 정렬
CREATE INDEX idx_balance_histories_user_created
ON balance_histories(user_id, created_at DESC);

-- 4. 주문-주문항목 조인 최적화
-- 주문 항목에서 주문 ID 기준 조회 최적화
CREATE INDEX idx_order_items_order_id
ON order_items(order_id);

-- 5. 상품별 주문 통계 최적화
-- 상품 ID + 생성일 복합 인덱스
CREATE INDEX idx_order_items_product_created
ON order_items(product_id, created_at);

-- 6. 쿠폰 성능 최적화 (이미 있다면 생략)
-- CREATE INDEX idx_coupons_availability
-- ON coupons(expired_at, issued_quantity, total_quantity);

-- 7. 사용자 쿠폰 최적화 (이미 있다면 생략)
-- CREATE UNIQUE INDEX idx_user_coupon_unique
-- ON user_coupons(user_id, coupon_id);

-- ==========================================
-- 인덱스 사용률 확인 쿼리
-- ==========================================

-- 생성된 인덱스 확인
SELECT
TABLE*NAME,
INDEX_NAME,
COLUMN_NAME,
SEQ_IN_INDEX,
CARDINALITY
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'hhplus'
AND INDEX_NAME LIKE 'idx*%'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- 테이블별 인덱스 크기 확인
SELECT
table_name,
ROUND(((index_length) / 1024 / 1024), 2) AS index_size_mb,
ROUND(((data_length) / 1024 / 1024), 2) AS data_size_mb,
table_rows
FROM information_schema.TABLES
WHERE table_schema = 'hhplus'
AND table_name IN ('order_items', 'orders', 'balance_histories', 'products')
ORDER BY index_size_mb DESC;
