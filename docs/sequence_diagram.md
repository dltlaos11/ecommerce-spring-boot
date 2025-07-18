# E-커머스 서비스 시퀀스 다이어그램

## 1. 주문 및 결제 프로세스 (핵심 플로우)

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant API as API Gateway
    participant OrderService as 주문 서비스
    participant PaymentService as 결제 서비스
    participant InventoryService as 재고 서비스
    participant CouponService as 쿠폰 서비스
    participant BalanceService as 잔액 서비스
    participant DB as 데이터베이스
    participant EventPublisher as 이벤트 발행자
    participant DataPlatform as 데이터 플랫폼

    Client->>API: POST /orders (주문 요청)
    API->>OrderService: 주문 생성 요청

    Note over OrderService: 1. 주문 유효성 검증
    OrderService->>InventoryService: 재고 확인 요청
    InventoryService->>DB: 상품 재고 조회 (비관적 락)
    DB-->>InventoryService: 재고 정보 반환

    alt 재고 부족
        InventoryService-->>OrderService: 재고 부족 오류
        OrderService-->>API: 주문 실패 응답
        API-->>Client: 400 Bad Request
    else 재고 충분
        InventoryService-->>OrderService: 재고 확인 완료

        Note over OrderService: 2. 쿠폰 적용 (선택사항)
        opt 쿠폰 사용
            OrderService->>CouponService: 쿠폰 유효성 검증
            CouponService->>DB: 사용자 쿠폰 조회
            DB-->>CouponService: 쿠폰 정보 반환
            alt 쿠폰 무효
                CouponService-->>OrderService: 쿠폰 무효 오류
                OrderService-->>API: 주문 실패 응답
                API-->>Client: 400 Bad Request
            else 쿠폰 유효
                CouponService-->>OrderService: 할인 금액 계산 완료
            end
        end

        Note over OrderService: 3. 주문 생성
        OrderService->>DB: 주문 정보 저장 (트랜잭션 시작)
        DB-->>OrderService: 주문 생성 완료

        Note over OrderService: 4. 결제 처리
        OrderService->>PaymentService: 결제 요청
        PaymentService->>BalanceService: 잔액 확인 및 차감
        BalanceService->>DB: 사용자 잔액 조회 (낙관적 락)
        DB-->>BalanceService: 잔액 정보 반환

        alt 잔액 부족
            BalanceService-->>PaymentService: 잔액 부족 오류
            PaymentService-->>OrderService: 결제 실패
            OrderService->>DB: 주문 취소 (롤백)
            OrderService-->>API: 결제 실패 응답
            API-->>Client: 400 Bad Request
        else 잔액 충분
            BalanceService->>DB: 잔액 차감 및 이력 저장
            DB-->>BalanceService: 잔액 업데이트 완료
            BalanceService-->>PaymentService: 결제 완료

            Note over PaymentService: 5. 재고 차감
            PaymentService->>InventoryService: 재고 차감 요청
            InventoryService->>DB: 상품 재고 차감
            DB-->>InventoryService: 재고 업데이트 완료
            InventoryService-->>PaymentService: 재고 차감 완료

            opt 쿠폰 사용된 경우
                PaymentService->>CouponService: 쿠폰 사용 처리
                CouponService->>DB: 쿠폰 상태 업데이트
                DB-->>CouponService: 쿠폰 사용 완료
            end

            PaymentService->>DB: 결제 정보 저장
            DB-->>PaymentService: 결제 저장 완료
            PaymentService-->>OrderService: 결제 성공

            Note over OrderService: 6. 주문 완료 처리
            OrderService->>DB: 주문 상태 업데이트 (트랜잭션 커밋)
            DB-->>OrderService: 주문 완료

            Note over OrderService: 7. 외부 데이터 전송
            OrderService->>EventPublisher: 주문 완료 이벤트 발행
            EventPublisher->>DataPlatform: 주문 데이터 전송 (비동기)
            Note over EventPublisher,DataPlatform: Mock/Fake 모듈로 구현<br/>추후 Kafka/Redis로 확장 가능

            OrderService-->>API: 주문 성공 응답
            API-->>Client: 201 Created
        end
    end
```

## 2. 선착순 쿠폰 발급 프로세스

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant API as API Gateway
    participant CouponService as 쿠폰 서비스
    participant LockManager as 락 관리자
    participant DB as 데이터베이스

    Client->>API: POST /coupons/{couponId}/issue (쿠폰 발급 요청)
    API->>CouponService: 쿠폰 발급 요청

    Note over CouponService: 1. 분산 락 획득 시도
    CouponService->>LockManager: 쿠폰 발급 락 요청
    LockManager-->>CouponService: 락 획득 성공/실패

    alt 락 획득 실패
        CouponService-->>API: 503 Service Unavailable
        API-->>Client: 잠시 후 다시 시도 요청
    else 락 획득 성공
        Note over CouponService: 2. 쿠폰 발급 가능 여부 확인
        CouponService->>DB: 쿠폰 정보 조회 (비관적 락)
        DB-->>CouponService: 쿠폰 정보 반환

        alt 발급 수량 초과
            CouponService->>LockManager: 락 해제
            CouponService-->>API: 409 Conflict (품절)
            API-->>Client: 쿠폰 품절 응답
        else 발급 가능
            Note over CouponService: 3. 중복 발급 검증
            CouponService->>DB: 사용자 쿠폰 보유 여부 확인
            DB-->>CouponService: 보유 여부 반환

            alt 이미 보유 중
                CouponService->>LockManager: 락 해제
                CouponService-->>API: 409 Conflict (중복)
                API-->>Client: 이미 보유 중 응답
            else 발급 가능
                Note over CouponService: 4. 쿠폰 발급 처리
                CouponService->>DB: 쿠폰 발급 수량 증가 & 사용자 쿠폰 생성 (트랜잭션)
                DB-->>CouponService: 발급 완료

                CouponService->>LockManager: 락 해제
                CouponService-->>API: 201 Created
                API-->>Client: 쿠폰 발급 성공
            end
        end
    end
```

## 3. 잔액 충전 프로세스

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant API as API Gateway
    participant BalanceService as 잔액 서비스
    participant DB as 데이터베이스

    Client->>API: POST /users/{userId}/balance/charge (잔액 충전 요청)
    API->>BalanceService: 잔액 충전 요청

    Note over BalanceService: 1. 충전 금액 유효성 검증
    alt 유효하지 않은 금액
        BalanceService-->>API: 400 Bad Request
        API-->>Client: 잘못된 요청
    else 유효한 금액
        Note over BalanceService: 2. 사용자 잔액 정보 조회/생성
        BalanceService->>DB: 사용자 잔액 조회 (낙관적 락)
        DB-->>BalanceService: 잔액 정보 반환

        Note over BalanceService: 3. 잔액 충전 처리
        BalanceService->>DB: 잔액 업데이트 & 충전 이력 저장 (트랜잭션)

        alt 동시성 충돌 (낙관적 락 실패)
            DB-->>BalanceService: 버전 충돌 오류
            BalanceService-->>API: 409 Conflict
            API-->>Client: 충돌 발생, 재시도 요청
        else 충전 성공
            DB-->>BalanceService: 충전 완료
            BalanceService-->>API: 200 OK
            API-->>Client: 충전 성공 응답
        end
    end
```

## 4. 인기 상품 조회 프로세스

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant API as API Gateway
    participant StatisticsService as 통계 서비스
    participant CacheManager as 캐시 관리자
    participant DB as 데이터베이스

    Client->>API: GET /products/popular (인기 상품 조회)
    API->>StatisticsService: 인기 상품 조회 요청

    Note over StatisticsService: 1. 캐시 확인
    StatisticsService->>CacheManager: 캐시된 인기 상품 조회
    CacheManager-->>StatisticsService: 캐시 데이터 반환/없음

    alt 캐시 히트
        StatisticsService-->>API: 200 OK (캐시 데이터)
        API-->>Client: 인기 상품 목록
    else 캐시 미스
        Note over StatisticsService: 2. 통계 쿼리 실행
        StatisticsService->>DB: 최근 3일간 판매량 기준 상위 5개 상품 조회
        Note over DB: 복잡한 집계 쿼리<br/>인덱스 활용 필수
        DB-->>StatisticsService: 통계 결과 반환

        Note over StatisticsService: 3. 캐시 업데이트
        StatisticsService->>CacheManager: 결과 캐싱 (TTL: 1시간)
        CacheManager-->>StatisticsService: 캐싱 완료

        StatisticsService-->>API: 200 OK
        API-->>Client: 인기 상품 목록
    end
```

## 설계 고려사항

### 1. 동시성 제어 전략

- **비관적 락**: 재고 차감, 쿠폰 발급 (데이터 정합성 중요)
- **낙관적 락**: 잔액 관리 (충돌 빈도 낮음)
- **분산 락**: 선착순 처리 (Redis/Database 레벨)

### 2. 트랜잭션 경계

- 주문-결제-재고차감을 하나의 트랜잭션으로 처리
- 외부 데이터 전송은 트랜잭션 외부에서 비동기 처리

### 3. 확장성 준비

- 인터페이스 기반 설계로 Redis, Kafka 도입 준비
- 이벤트 발행자를 통한 느슨한 결합
- 캐시 레이어 추상화

### 4. 장애 처리

- 재시도 메커니즘 (쿠폰 발급, 외부 연동)
- Circuit Breaker 패턴 적용 가능
- 우아한 성능 저하 (인기 상품 캐시)
