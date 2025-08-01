# STEP08: DB 성능 최적화 분석 및 개선 보고서

## 📋 목차

1. [성능 저하 가능성 기능 식별](#1-성능-저하-가능성-기능-식별)
2. [쿼리 실행계획 이론적 분석](#2-쿼리-실행계획-이론적-분석)
3. [인덱스 설계 및 최적화 방안](#3-인덱스-설계-및-최적화-방안)
4. [성능 개선 예상 효과](#4-성능-개선-예상-효과)
5. [구현된 최적화 방안](#5-구현된-최적화-방안)
6. [향후 테스트 계획](#6-향후-테스트-계획)

---

## 1. 성능 저하 가능성 기능 식별

### 1.1 고위험 성능 저하 시나리오

#### 🔴 **HIGH: 인기 상품 통계 조회**

- **기능**: `ProductService.getPopularProducts()`
- **문제점**:
  - `OrderItemRepository.findAll()`로 전체 테이블 스캔
  - 애플리케이션 메모리에서 날짜 필터링 수행
  - Java Stream으로 GROUP BY 처리
  - 대용량 데이터 시 메모리 부족 위험
- **예상 데이터량**: 주문 100만건 시 order_items 500만건
- **현재 코드**:

```java
// ProductService.java - 문제가 있는 현재 구현
List<OrderItem> recentOrderItems = orderItemRepository.findAll().stream()
    .filter(item -> item.getCreatedAt().isAfter(startDate))  // 메모리에서 필터링
    .collect(Collectors.toList());
```

#### 🟡 **MEDIUM: 사용자별 주문 목록 조회**

- **기능**: `OrderService.getUserOrders()`
- **문제점**:
  - `user_id` 컬럼에 인덱스 부족으로 풀스캔 가능성
  - Repository 2번 호출로 N+1 문제 발생
- **현재 코드**:

```java
// OrderService.java - N+1 문제 발생 코드
List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
return orders.stream()
    .map(order -> {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId()); // N번 실행
        return convertToOrderResponse(order, orderItems);
    })
    .toList();
```

#### 🟡 **MEDIUM: 잔액 이력 조회**

- **기능**: `BalanceService.getBalanceHistories()`
- **문제점**:
  - `user_id`와 `created_at` 복합 인덱스 부족
  - LIMIT 없이 대량 데이터 조회 가능성

### 1.2 성능 측정 기준 설정

- **목표 응답시간**: 95% 요청이 500ms 이내
- **동시 사용자**: 1,000명
- **데이터량**: 주문 100만건, 사용자 10만명 기준

---

## 2. 쿼리 실행계획 이론적 분석

### 2.1 인기 상품 통계 쿼리 분석

#### **문제가 예상되는 쿼리**

```sql
-- 현재 JPA가 생성하는 쿼리 (예상)
SELECT * FROM order_items;  -- 전체 조회 후 애플리케이션에서 처리
```

**예상 실행계획:**
| id | select_type | table | type | key | rows | Extra |
|----|-------------|-------|------|-----|------|--------|
| 1 | SIMPLE | order_items | **ALL** | NULL | **500,000+** | Using filesort |

**⚠️ 예상 문제점:**

- **type: ALL** → 풀테이블 스캔 불가피
- **rows: 500,000+** → 모든 데이터 로딩
- **메모리 부족** → 대용량 데이터 시 OutOfMemoryError 위험

### 2.2 사용자 주문 목록 쿼리 분석

#### **현재 생성되는 쿼리들**

```sql
-- 1. 주문 목록 조회
SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC;

-- 2. 각 주문별 항목 조회 (N번 실행)
SELECT * FROM order_items WHERE order_id = ?;
```

**예상 실행계획 (user_id 인덱스 없는 경우):**
| 쿼리 | type | key | rows | 문제점 |
|------|------|-----|------|--------|
| orders | ALL | NULL | 100,000 | 풀스캔 |
| order_items | ref | PRIMARY | 5 | N+1 문제 |

---

## 3. 인덱스 설계 및 최적화 방안

### 3.1 핵심 인덱스 설계 전략

#### **1) 인기 상품 통계 최적화를 위한 인덱스**

```sql
-- 기본 복합 인덱스: 날짜 범위 + 그룹핑
CREATE INDEX idx_order_items_created_product
ON order_items(created_at, product_id);

-- 커버링 인덱스: 테이블 접근 제거
CREATE INDEX idx_order_items_stats_covering
ON order_items(created_at, product_id, quantity, subtotal, product_name, product_price);
```

**설계 근거:**

- **선두 컬럼**: `created_at` (WHERE 절 날짜 범위 검색)
- **두 번째**: `product_id` (GROUP BY 키)
- **커버링 컬럼들**: SELECT 절 모든 컬럼 포함

#### **2) 사용자 주문 조회 최적화**

```sql
-- 사용자별 주문 목록 조회 최적화
CREATE INDEX idx_orders_user_created
ON orders(user_id, created_at DESC);
```

**효과:**

- `user_id` 필터링 최적화
- `created_at DESC` 정렬 최적화
- filesort 연산 제거

#### **3) 잔액 이력 조회 최적화**

```sql
-- 사용자별 잔액 이력 조회 최적화
CREATE INDEX idx_balance_histories_user_created
ON balance_histories(user_id, created_at DESC);
```

### 3.2 쿼리 구조 개선 방안

#### **개선된 인기 상품 통계 로직**

```java
// 개선 방안 1: Repository에서 직접 집계 쿼리 실행
@Query("""
    SELECT new kr.hhplus.be.server.product.dto.PopularProductStats(
        oi.productId,
        oi.productName,
        oi.productPrice,
        SUM(oi.quantity),
        SUM(oi.subtotal)
    )
    FROM OrderItem oi
    WHERE oi.createdAt >= :startDate
    GROUP BY oi.productId, oi.productName, oi.productPrice
    ORDER BY SUM(oi.quantity) DESC
    """)
List<PopularProductStats> findPopularProducts(
    @Param("startDate") LocalDateTime startDate,
    Pageable pageable
);
```

#### **N+1 문제 해결 방안**

```yaml
# application.yml - Batch Fetch 설정
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

```java
// 개선된 주문 조회 로직
@Query("""
    SELECT DISTINCT o FROM Order o
    LEFT JOIN FETCH o.orderItems
    WHERE o.userId = :userId
    ORDER BY o.createdAt DESC
    """)
List<Order> findByUserIdWithItems(@Param("userId") Long userId);
```

---

## 4. 성능 개선 예상 효과

### 4.1 인기 상품 통계 쿼리 개선 예상

#### **개선 전 (현재)**

- **데이터 처리**: 전체 500만건 메모리 로딩
- **예상 응답시간**: 10-30초 (메모리 부족 시 실패)
- **CPU 사용률**: 90%+
- **메모리 사용량**: 1GB+ (OOM 위험)

#### **개선 후 (인덱스 적용)**

- **데이터 처리**: 필요한 30일 데이터만 (약 50만건)
- **예상 응답시간**: 0.5-2초
- **CPU 사용률**: 30-50%
- **메모리 사용량**: 100MB 이하

### 4.2 사용자 주문 조회 개선 예상

#### **개선 전**

- **스캔 방식**: 전체 orders 테이블 스캔
- **쿼리 수**: 1 + N번 (N+1 문제)
- **예상 응답시간**: 1-3초

#### **개선 후**

- **스캔 방식**: 인덱스 범위 스캔
- **쿼리 수**: 1-2번 (Batch Fetch)
- **예상 응답시간**: 0.05-0.2초

---

## 5. 구현된 최적화 방안

### 5.1 구현 완료 항목

#### ✅ **1. 성능 테스트 인프라 구축**

- `PerformanceTestDataLoader`: 대용량 테스트 데이터 생성
- `PerformanceTestController`: 성능 비교 API 엔드포인트
- `application-performance.yml`: 성능 테스트 전용 설정

#### ✅ **2. 인덱스 생성 스크립트 작성**

```sql
-- 구현된 최적화 인덱스들
CREATE INDEX idx_order_items_created_product ON order_items(created_at, product_id);
CREATE INDEX idx_order_items_stats_covering ON order_items(created_at, product_id, quantity, subtotal, product_name, product_price);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
CREATE INDEX idx_balance_histories_user_created ON balance_histories(user_id, created_at DESC);
```

#### ✅ **3. 성능 모니터링 API 구현**

- 실행계획 분석 API
- 인덱스 사용률 확인 API
- 테이블 통계 정보 API

### 5.2 현재 아키텍처의 성능 이슈

#### **🔧 Entity-Domain 통합의 장단점**

**장점:**

- 코드 단순화로 개발 속도 향상
- 변환 로직 제거로 오버헤드 감소

**성능 고려사항:**

- JPA 연관관계 매핑 제거로 N+1 문제 회피
- Repository 2번 호출 방식으로 명시적 쿼리 제어

#### **🔧 현재 Repository 패턴의 특징**

```java
// 현업 스타일: 연관관계 없이 명시적 호출
public List<OrderResponse> getUserOrders(Long userId) {
    List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

    return orders.stream()
        .map(order -> {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
            return convertToOrderResponse(order, orderItems);
        })
        .toList();
}
```

---

## 6. 향후 테스트 계획

### 6.1 성능 테스트 시나리오

#### **Phase 1: 기준 성능 측정**

1. 대용량 데이터 생성 (100만건+ 주문 데이터)
2. 인덱스 적용 전 성능 측정
3. 병목 지점 식별 및 문서화

#### **Phase 2: 최적화 적용 및 검증**

1. 인덱스 순차적 적용
2. 각 단계별 성능 측정
3. 개선 효과 정량적 분석

#### **Phase 3: 부하 테스트**

1. 동시 사용자 1,000명 시뮬레이션
2. 응답시간 95% 백분위수 측정
3. 시스템 리소스 사용률 모니터링

### 6.2 측정 지표

#### **응답시간 지표**

- 평균 응답시간
- 95% 백분위수 응답시간
- 최대 응답시간

#### **처리량 지표**

- TPS (Transactions Per Second)
- 동시 처리 가능 요청 수
- 에러율

#### **시스템 지표**

- CPU 사용률
- 메모리 사용량
- DB 연결 풀 사용률
- 슬로우 쿼리 발생 빈도
