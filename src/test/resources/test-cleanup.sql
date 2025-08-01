-- 테스트 데이터 정리 스크립트
-- 외래키 관계를 고려한 순서로 삭제

-- 1. 주문 관련 데이터 삭제 (가장 하위)
DELETE FROM order_items WHERE order_id > 0;
DELETE FROM payments WHERE id > 0;
DELETE FROM orders WHERE id > 0;

-- 2. 쿠폰 관련 데이터 삭제
DELETE FROM user_coupons WHERE id > 0;

-- 3. 잔액 관련 데이터 삭제
DELETE FROM balance_histories WHERE id > 0;
DELETE FROM user_balances WHERE id > 0;

-- 4. 상품 데이터는 DataLoader가 생성한 것만 유지하고 테스트 데이터만 삭제
DELETE FROM products WHERE name LIKE '%테스트%' OR name LIKE '%test%' OR name LIKE '%TEST%';

-- 5. 쿠폰 데이터도 DataLoader 데이터 유지
DELETE FROM coupons WHERE name LIKE '%테스트%' OR name LIKE '%test%' OR name LIKE '%TEST%';

-- AUTO_INCREMENT 값 리셋 (optional)
-- ALTER TABLE user_balances AUTO_INCREMENT = 1;
-- ALTER TABLE balance_histories AUTO_INCREMENT = 1;