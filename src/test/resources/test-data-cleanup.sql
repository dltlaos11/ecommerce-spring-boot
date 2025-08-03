-- 테스트 데이터 정리 (DataLoader가 생성한 초기 데이터 제외)
DELETE FROM balance_histories WHERE id > 0;
DELETE FROM user_balances WHERE id > 0;
DELETE FROM user_coupons WHERE id > 0;
DELETE FROM order_items WHERE id > 0;
DELETE FROM orders WHERE id > 0;
DELETE FROM payments WHERE id > 0;

-- 테스트에서 생성한 상품만 삭제 (초기 10개 제외)
DELETE FROM products WHERE id > 10;

-- 테스트에서 생성한 쿠폰만 삭제 (초기 6개 제외)
DELETE FROM coupons WHERE id > 6;

-- AUTO_INCREMENT 리셋 (테스트 데이터만)
ALTER TABLE balance_histories AUTO_INCREMENT = 1;
ALTER TABLE user_balances AUTO_INCREMENT = 1;
ALTER TABLE user_coupons AUTO_INCREMENT = 1;
ALTER TABLE order_items AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE payments AUTO_INCREMENT = 1;
ALTER TABLE products AUTO_INCREMENT = 11;
ALTER TABLE coupons AUTO_INCREMENT = 7;