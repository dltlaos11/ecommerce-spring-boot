-- ==========================================
-- DB 성능 분석용 SQL 쿼리 모음
-- ==========================================

-- 1. 인기 상품 통계 쿼리 (개선 전)
-- 문제: 전체 order_items 테이블 풀스캔
EXPLAIN SELECT 
    oi.product_id,
    oi.product_name,
    oi.product_price,
    SUM(oi.quantity) as total_quantity,
    SUM(oi.subtotal) as total_amount,
    COUNT(*) as order_count
FROM order_items oi 
WHERE oi.created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY oi.product_id, oi.product_name, oi.product_price
ORDER BY total_quantity DESC 
LIMIT 5;

-- 2. 사용자별 주문 목록 조회 (개선 전)
-- 문제: user_id에 인덱스 없어서 풀스캔
EXPLAIN SELECT 
    o.id, o.order_number, o.total_amount, o.final_amount, o.created_at
FROM orders o 
WHERE o.user_id = 12345 
ORDER BY o.created_at DESC 
LIMIT 10;

-- 3. 잔액 이력 조회 (개선 전)
-- 문제: user_id + created_at 복합 인덱스 없음
EXPLAIN SELECT 
    bh.transaction_type, bh.amount, bh.balance_after, bh.created_at
FROM balance_histories bh 
WHERE bh.user_id = 12345 
ORDER BY bh.created_at DESC 
LIMIT 10;

-- ==========================================
-- 성능 최적화 인덱스 생성
-- ==========================================

-- 1. 인기 상품 통계 최적화
-- 복합 인덱스: 날짜 + 상품ID
CREATE INDEX idx_order_items_created_product 
ON order_items(created_at, product_id);

-- 커버링 인덱스: SELECT 절 모든 컬럼 포함
CREATE INDEX idx_order_items_stats_covering 
ON order_items(created_at, product_id, quantity, subtotal, product_name, product_price);

-- 2. 사용자 주문 목록 최적화
CREATE INDEX idx_orders_user_created 
ON orders(user_id, created_at DESC);

-- 3. 잔액 이력 최적화
CREATE INDEX idx_balance_histories_user_created 
ON balance_histories(user_id, created_at DESC);

-- 4. 기존 인덱스 개선 (있다면 삭제 후 재생성)
-- DROP INDEX idx_order_items_created_product ON order_items;
-- DROP INDEX idx_orders_user_created ON orders;

-- ==========================================
-- 개선된 쿼리 (인덱스 힌트 포함)
-- ==========================================

-- 1. 최적화된 인기 상품 통계 쿼리
EXPLAIN SELECT /*+ USE_INDEX(oi, idx_order_items_stats_covering) */
    oi.product_id,
    oi.product_name,
    oi.product_price,
    SUM(oi.quantity) as total_quantity,
    SUM(oi.subtotal) as total_amount,
    COUNT(*) as order_count
FROM order_items oi 
WHERE oi.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY oi.product_id, oi.product_name, oi.product_price
ORDER BY total_quantity DESC 
LIMIT 5;

-- 2. 최적화된 사용자 주문 목록 쿼리
EXPLAIN SELECT /*+ USE_INDEX(o, idx_orders_user_created) */
    o.id, o.order_number, o.total_amount, o.final_amount, o.created_at
FROM orders o 
WHERE o.user_id = 12345 
ORDER BY o.created_at DESC 
LIMIT 10;

-- 3. 최적화된 잔액 이력 쿼리
EXPLAIN SELECT /*+ USE_INDEX(bh, idx_balance_histories_user_created) */
    bh.transaction_type, bh.amount, bh.balance_after, bh.created_at
FROM balance_histories bh 
WHERE bh.user_id = 12345 
ORDER BY bh.created_at DESC 
LIMIT 10;

-- ==========================================
-- 성능 측정 쿼리
-- ==========================================

-- 1. 테이블 크기 확인
SELECT 
    table_name,
    table_rows,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES 
WHERE table_schema = 'hhplus'
ORDER BY table_rows DESC;

-- 2. 인덱스 사용률 확인
SELECT 
    table_schema,
    table_name,
    index_name,
    cardinality,
    ROUND(stat_value, 2) as pages
FROM information_schema.STATISTICS 
WHERE table_schema = 'hhplus'
ORDER BY cardinality DESC;

-- 3. 슬로우 쿼리 확인 (slow query log 활성화 필요)
-- SET GLOBAL slow_query_log = 'ON';
-- SET GLOBAL long_query_time = 1;
-- SHOW VARIABLES LIKE 'slow_query_log%';

-- 4. 쿼리 실행 시간 측정
SET profiling = 1;

-- 테스트할 쿼리 실행
SELECT 
    oi.product_id,
    SUM(oi.quantity) as total_quantity
FROM order_items oi 
WHERE oi.created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY oi.product_id
ORDER BY total_quantity DESC 
LIMIT 5;

-- 실행 시간 확인
SHOW PROFILES;
SHOW PROFILE FOR QUERY 1;

-- ==========================================
-- 실시간 통계 테이블 (고도화 방안)
-- ==========================================

-- 일별 상품 판매 통계 테이블
CREATE TABLE daily_product_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_price DECIMAL(10,2) NOT NULL,
    stats_date DATE NOT NULL,
    total_quantity INT DEFAULT 0,
    total_amount DECIMAL(15,2) DEFAULT 0,
    order_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_product_date (product_id, stats_date),
    INDEX idx_stats_date (stats_date),
    INDEX idx_product_stats (product_id, stats_date),
    INDEX idx_stats_quantity (stats_date, total_quantity DESC)
);

-- 일별 통계 집계 프로시저
DELIMITER //
CREATE PROCEDURE AggregrateDailyStats(IN target_date DATE)
BEGIN
    INSERT INTO daily_product_stats (
        product_id, product_name, product_price, stats_date,
        total_quantity, total_amount, order_count
    )
    SELECT 
        oi.product_id,
        oi.product_name,
        oi.product_price,
        DATE(oi.created_at) as stats_date,
        SUM(oi.quantity) as total_quantity,
        SUM(oi.subtotal) as total_amount,
        COUNT(*) as order_count
    FROM order_items oi
    WHERE DATE(oi.created_at) = target_date
    GROUP BY oi.product_id, oi.product_name, oi.product_price, DATE(oi.created_at)
    ON DUPLICATE KEY UPDATE
        total_quantity = VALUES(total_quantity),
        total_amount = VALUES(total_amount),
        order_count = VALUES(order_count),
        updated_at = CURRENT_TIMESTAMP;
END //
DELIMITER ;

-- 실시간 통계를 이용한 빠른 인기 상품 조회
SELECT 
    product_id,
    product_name,
    product_price,
    SUM(total_quantity) as total_quantity,
    SUM(total_amount) as total_amount,
    SUM(order_count) as order_count
FROM daily_product_stats 
WHERE stats_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY product_id, product_name, product_price
ORDER BY total_quantity DESC
LIMIT 5;

-- ==========================================
-- 성능 모니터링 쿼리
-- ==========================================

-- 1. 현재 실행 중인 쿼리 확인
SELECT 
    id,
    user,
    host,
    db,
    command,
    time,
    state,
    LEFT(info, 100) as query_preview
FROM information_schema.PROCESSLIST 
WHERE state != '' 
ORDER BY time DESC;

-- 2. InnoDB 상태 확인
SHOW ENGINE INNODB STATUS;

-- 3. 버퍼 풀 히트율 확인
SELECT 
    (1 - (Innodb_buffer_pool_reads / Innodb_buffer_pool_read_requests)) * 100 
    AS buffer_pool_hit_rate
FROM 
    (SELECT variable_value AS Innodb_buffer_pool_reads 
     FROM performance_schema.global_status 
     WHERE variable_name = 'Innodb_buffer_pool_reads') AS reads,
    (SELECT variable_value AS Innodb_buffer_pool_read_requests 
     FROM performance_schema.global_status 
     WHERE variable_name = 'Innodb_buffer_pool_read_requests') AS requests;

-- 4. 테이블별 통계 정보 업데이트
ANALYZE TABLE order_items, orders, balance_histories;

-- ==========================================
-- 벤치마크 테스트 쿼리
-- ==========================================

-- 반복 실행으로 평균 성능 측정
DELIMITER //
CREATE PROCEDURE BenchmarkQuery(IN iterations INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE start_time BIGINT;
    DECLARE end_time BIGINT;
    
    SET start_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    
    WHILE i < iterations DO
        SELECT 
            product_id,
            SUM(quantity) as total_quantity
        FROM order_items 
        WHERE created_at > DATE_SUB(NOW(), INTERVAL 30 DAY)
        GROUP BY product_id
        ORDER BY total_quantity DESC 
        LIMIT 5;
        
        SET i = i + 1;
    END WHILE;
    
    SET end_time = UNIX_TIMESTAMP(NOW(6)) * 1000000 + MICROSECOND(NOW(6));
    
    SELECT 
        iterations as iterations,
        ROUND((end_time - start_time) / 1000, 2) as total_time_ms,
        ROUND((end_time - start_time) / 1000 / iterations, 2) as avg_time_ms;
END //
DELIMITER ;

-- 벤치마크 실행
-- CALL BenchmarkQuery(10);