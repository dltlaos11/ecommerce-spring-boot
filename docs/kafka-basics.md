# Kafka 기본 개념 및 아키텍처

## 🎯 학습 목표

이 문서는 STEP 17-18 진행을 위한 Apache Kafka 핵심 개념을 정리하고, 실제 e-commerce 프로젝트에 적용하는 방법을 설명합니다.

## 1. Apache Kafka 개요

Apache Kafka는 실시간 스트리밍 데이터를 위한 **분산 이벤트 스트리밍 플랫폼**입니다.

### 주요 특징

- **높은 처리량**: 초당 수백만 개의 메시지 처리
- **확장성**: 수평적 확장을 통한 클러스터 구성
- **내결함성**: 데이터 복제를 통한 장애 대응
- **영속성**: 디스크에 메시지 저장으로 데이터 보존

---

## 2. Kafka 핵심 구성 요소

### 2.1 Broker (브로커)

**역할**: Kafka 클러스터를 구성하는 개별 서버 노드

**주요 특징**:

- **메시지 저장**: 토픽의 파티션 데이터를 디스크에 영구 저장
- **클러스터 구성**: 여러 브로커가 함께 Kafka 클러스터를 형성
- **리더/팔로워 관계**: 각 파티션마다 하나의 리더 브로커와 여러 팔로워 브로커
- **고가용성**: 브로커 장애 시 자동 장애조치(Failover)

**실제 동작**:

```
브로커-1: order-completed-partition-0 (Leader)
브로커-2: order-completed-partition-1 (Leader)
브로커-3: order-completed-partition-2 (Leader)
```

### 2.2 Topic (토픽)

**역할**: 메시지를 분류하는 논리적 카테고리 (데이터베이스의 테이블과 유사)

**주요 특징**:

- **논리적 그룹화**: 관련된 메시지들을 하나의 토픽으로 묶음
- **다중 파티션**: 하나의 토픽은 여러 파티션으로 구성
- **확장 가능**: 파티션 수를 증가시켜 처리량 향상 (단, 감소 불가)
- **보존 정책**: 메시지 보관 기간 및 크기 제한 설정

**본 프로젝트 토픽 설계**:

```java
// 도메인별 토픽 분리 전략
"order-completed"     // 주문 완료 이벤트
"coupon-issue"        // 쿠폰 발급 이벤트
"user-activity"       // 사용자 활동 로그
"balance-activity"    // 잔액 변경 이벤트
"product-activity"    // 상품 재고 변경 이벤트
```

### 2.3 Partition (파티션)

**역할**: 토픽 내에서 메시지를 물리적으로 분산 저장하는 최소 단위

**핵심 개념**:

- **순서 보장**: 같은 파티션 내 메시지는 순서 보장 (FIFO)
- **병렬 처리**: 파티션 수 = 최대 병렬 Consumer 수
- **불변성**: 한번 저장된 메시지는 수정 불가 (Immutable)
- **오프셋**: 각 메시지는 파티션 내 고유한 순번(offset) 부여

**파티션 분산 예시**:

```
Topic: order-completed (3개 파티션)

Partition-0: [msg1] [msg4] [msg7] ...
Partition-1: [msg2] [msg5] [msg8] ...
Partition-2: [msg3] [msg6] [msg9] ...
     ↓         ↓         ↓
 Consumer-A Consumer-B Consumer-C
```

**파티션 키 결정 로직**:

```java
// 순서 보장이 필요한 경우
case "BALANCE_CHARGED" -> "user:" + event.getAggregateId();
// → 같은 사용자의 잔액 변경은 항상 같은 파티션

// 로드밸런싱이 우선인 경우
case "ORDER_COMPLETED" -> null;
// → 라운드로빈으로 균등 분산
```

### 2.4 Producer (프로듀서)

**역할**: 메시지를 생성하여 Kafka 토픽으로 발행하는 클라이언트

**주요 특징**:

- **배치 처리**: 여러 메시지를 묶어서 효율적으로 전송
- **압축**: 네트워크 대역폭 절약을 위한 메시지 압축
- **파티셔닝**: 메시지 키 기반으로 적절한 파티션 선택
- **재시도**: 전송 실패 시 자동 재시도

**프로듀서 메시지 전송 과정**:

```java
// 1. 메시지 생성 및 직렬화
Message<DomainEvent> message = MessageBuilder
    .withPayload(event)
    .setHeader(KafkaHeaders.TOPIC, "order-completed")
    .setHeader(KafkaHeaders.KEY, partitionKey)
    .build();

// 2. 파티션 선택
// - 키가 있으면: hash(key) % partition_count
// - 키가 없으면: 라운드로빈

// 3. 배치에 추가하고 전송
kafkaTemplate.send(message);
```

**본 프로젝트 Producer 구현**:

```java
// 도메인 특화된 이벤트 발행 (RedisCouponService에서)
@Service
public class RedisCouponService {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public void issueCoupon(Long userId, Long couponId) {
        // Redis 빠른 검증 및 처리
        // ...
        
        // 트랜잭션 커밋 후 Kafka 이벤트 발행
        CouponIssueEvent event = CouponIssueEvent.create(couponId, userId, requestId);
        eventPublisher.publishEvent(event);
    }
}

// @TransactionalEventListener를 통한 실제 Kafka 발행
@Component
public class TransactionalKafkaEventHandler {
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponIssueEvent(CouponIssueEvent event) {
        kafkaTemplate.send("coupon-issue", event);
    }
}
```

### 2.5 Consumer (컨슈머)

**역할**: 토픽에서 메시지를 읽어와 처리하는 클라이언트

**주요 특징**:

- **풀(Pull) 모델**: 컨슈머가 능동적으로 메시지를 가져옴
- **오프셋 관리**: 처리한 메시지의 위치를 추적
- **그룹 소속**: Consumer Group을 통한 협력적 소비
- **리밸런싱**: 그룹 내 컨슈머 변화 시 파티션 재할당

**Consumer 메시지 처리 과정**:

```java
@KafkaListener(topics = "order-completed", groupId = "data-platform-consumer-group")
public void handleOrderCompleted(
    @Payload OrderDataPlatformEvent event,
    @Header(name = KafkaHeaders.RECEIVED_PARTITION) int partition,
    @Header(name = KafkaHeaders.OFFSET) long offset) {

    // 1. 메시지 수신 및 역직렬화
    // 2. 비즈니스 로직 처리
    // 3. 오프셋 커밋 (처리 완료 표시)
}
```

### 2.6 Consumer Group (컨슈머 그룹)

**역할**: 동일한 토픽을 소비하는 Consumer들의 논리적 그룹

**핵심 원리**:

- **로드밸런싱**: 그룹 내 Consumer들이 파티션을 나누어 처리
- **중복 방지**: 각 파티션은 그룹 내 하나의 Consumer만 할당
- **장애 복구**: Consumer 장애 시 다른 Consumer가 파티션 인계
- **확장성**: Consumer 추가로 처리 성능 향상

**Consumer Group 동작 예시**:

```
Topic: coupon-issue (3개 파티션)

Consumer Group: coupon-issue-consumer-group
├─ Consumer-1 → Partition-0 처리
├─ Consumer-2 → Partition-1 처리
└─ Consumer-3 → Partition-2 처리

Consumer-2 장애 발생 시:
├─ Consumer-1 → Partition-0, Partition-1 처리
└─ Consumer-3 → Partition-2 처리
```

## 2.7 구성 요소 간 상호 관계

### 전체 데이터 흐름

```
Producer → Broker → Topic → Partition → Consumer Group → Consumer
    ↓         ↓        ↓        ↓            ↓           ↓
  메시지     저장     분류     분산        협력        처리
  생성      관리     채널     저장        소비        로직
```

### 확장성과 성능의 관계

- **파티션 수 ↑** = **병렬 처리 능력 ↑** = **Consumer 수 ↑**
- **브로커 수 ↑** = **저장 용량 ↑** = **가용성 ↑**
- **배치 크기 ↑** = **처리량 ↑** (단, 지연시간 ↑)

### 순서 보장과 성능의 트레이드오프

```java
// Case 1: 순서 보장 (성능 제한)
// 단일 파티션 사용 → 순차 처리 → 낮은 처리량
kafkaTemplate.send("coupon-issue", couponId.toString(), event);

// Case 2: 성능 우선 (순서 포기)
// 다중 파티션 사용 → 병렬 처리 → 높은 처리량
kafkaTemplate.send("order-completed", event);
```

---

## 3. Producer, Partition, Consumer 수에 따른 데이터 흐름 분석

### 3.1 시나리오별 성능 분석

#### 시나리오 1: 1 Producer → 1 Partition → 1 Consumer

```
Producer-1 → Topic (1 Partition) → Consumer-1

특징:
- 순서 보장: 완벽 (FIFO 보장)
- 처리량: 낮음 (순차 처리)
- 장애 영향: 높음 (단일 장애점)
- 사용 사례: 중요한 순서 보장이 필요한 경우
```

#### 시나리오 2: 1 Producer → 3 Partition → 3 Consumer

```
                    ┌─ Partition-0 → Consumer-1
Producer-1 ────────┼─ Partition-1 → Consumer-2
                    └─ Partition-2 → Consumer-3

특징:
- 순서 보장: 파티션 내에서만 보장
- 처리량: 높음 (3배 병렬 처리)
- 장애 영향: 중간 (1/3만 영향)
- 사용 사례: 성능이 중요한 경우
```

#### 시나리오 3: 3 Producer → 3 Partition → 3 Consumer

```
Producer-1 ────┐   ┌─ Partition-0 → Consumer-1
Producer-2 ────┼───┼─ Partition-1 → Consumer-2
Producer-3 ────┘   └─ Partition-2 → Consumer-3

특징:
- 순서 보장: 키 기반 파티셔닝으로 제어
- 처리량: 매우 높음 (생산/소비 모두 병렬)
- 장애 영향: 낮음 (분산 처리)
- 사용 사례: 대용량 트래픽 처리
```

### 3.2 Consumer 수와 파티션 수의 관계

#### 최적 구성: Consumer 수 = Partition 수

```java
// 3개 파티션, 3개 Consumer (최적)
@KafkaListener(topics = "coupon-issue", groupId = "coupon-consumer-group")
public void handleCoupon1(CouponIssueEvent event) { ... }

@KafkaListener(topics = "coupon-issue", groupId = "coupon-consumer-group")
public void handleCoupon2(CouponIssueEvent event) { ... }

@KafkaListener(topics = "coupon-issue", groupId = "coupon-consumer-group")
public void handleCoupon3(CouponIssueEvent event) { ... }
```

#### 비효율적 구성: Consumer 수 > Partition 수

```
3개 파티션, 5개 Consumer
┌─ Partition-0 → Consumer-1 ✅
├─ Partition-1 → Consumer-2 ✅
├─ Partition-2 → Consumer-3 ✅
├─ Consumer-4 (유휴 상태) ❌
└─ Consumer-5 (유휴 상태) ❌
```

#### 과부하 구성: Consumer 수 < Partition 수

```
5개 파티션, 2개 Consumer
┌─ Partition-0 ┐
├─ Partition-1 ├─ Consumer-1 (과부하) ⚠️
├─ Partition-2 ┘
├─ Partition-3 ┐
└─ Partition-4 ├─ Consumer-2 (과부하) ⚠️
```

### 3.3 실제 처리량 계산 예시

#### 쿠폰 발급 시스템 기준

```java
// 기본 성능 지표
- 단일 Consumer 처리량: 1,000 TPS
- 파티션당 메시지 크기: 1KB
- 네트워크 대역폭: 100MB/s

// 시나리오별 예상 처리량
1 Partition + 1 Consumer = 1,000 TPS
3 Partition + 3 Consumer = 3,000 TPS
5 Partition + 5 Consumer = 5,000 TPS
```

#### 병목 구간 분석

```java
// Producer 병목 (생산 < 소비)
Producer: 2,000 TPS → 3 Partition → 3 Consumer: 3,000 TPS 가능
결과: 2,000 TPS (Producer 제한)

// Consumer 병목 (생산 > 소비)
Producer: 5,000 TPS → 3 Partition → 3 Consumer: 3,000 TPS 가능
결과: 3,000 TPS (Consumer 제한) + Consumer Lag 발생
```

### 3.4 리밸런싱(Rebalancing) 동작 과정

#### Consumer 추가 시

```
초기 상태 (2 Consumer):
├─ Consumer-1: Partition-0, Partition-1
└─ Consumer-2: Partition-2

Consumer-3 추가 후:
├─ Consumer-1: Partition-0
├─ Consumer-2: Partition-1
└─ Consumer-3: Partition-2
```

#### Consumer 장애 시

```
정상 상태:
├─ Consumer-1: Partition-0
├─ Consumer-2: Partition-1 ❌ (장애)
└─ Consumer-3: Partition-2

리밸런싱 후:
├─ Consumer-1: Partition-0, Partition-1
└─ Consumer-3: Partition-2
```

### 3.5 파티션 증설 시 고려사항

#### 파티션 증설 효과

```java
// Before: 3 Partition
Topic: order-events
├─ Partition-0: 1,000 msg/sec
├─ Partition-1: 1,000 msg/sec
└─ Partition-2: 1,000 msg/sec
Total: 3,000 msg/sec

// After: 6 Partition
Topic: order-events
├─ Partition-0~2: 기존 메시지 유지
├─ Partition-3: 새 메시지 분산
├─ Partition-4: 새 메시지 분산
└─ Partition-5: 새 메시지 분산
Total: 6,000 msg/sec (이론치)
```

#### 주의사항

```java
// ⚠️ 키 기반 메시지는 파티션 변경 시 순서 보장 깨질 수 있음
// Before: hash("user123") % 3 = Partition-1
// After:  hash("user123") % 6 = Partition-3 (다른 파티션!)

// 해결 방안: Sticky Partitioning 또는 Custom Partitioner 사용
```

## 4. 메시지 순서 보장 전략

### 4.1 순서 보장이 필요한 경우

```java
// 사용자별 잔액 변경 - 순서 보장 필요
case "BALANCE_CHARGED" -> "user:" + event.getAggregateId();
```

### 4.2 로드밸런싱이 우선인 경우

```java
// 주문 완료 - 순서 보장 불필요, 성능 우선
case "ORDER_COMPLETED" -> null; // 라운드로빈 방식
```

---

## 5. 이벤트 발행 패턴

### 5.1 트랜잭션 커밋 후 발행 (권장)

```java
// 도메인 서비스에서 Spring ApplicationEvent 발행
@Service
public class RedisCouponService {
    
    public void issueCoupon(Long userId, Long couponId) {
        // 비즈니스 로직 처리 후
        CouponIssueEvent event = CouponIssueEvent.create(couponId, userId, requestId);
        applicationEventPublisher.publishEvent(event);
    }
}

// 트랜잭션 커밋 후 실제 Kafka 발행
@Component
public class TransactionalKafkaEventHandler {
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCouponIssueEvent(CouponIssueEvent event) {
        // 실제 Kafka 발행은 트랜잭션 커밋 후
        kafkaTemplate.send("coupon-issue", event.couponId().toString(), event);
    }
}
```

**멘토링 핵심 원칙**:

> 커밋이 안됐는데 이미 메시지를 발행하면, 구독 서비스는 id를 보고 발행 서비스에 데이터 요청을 다시 진행 -> '없는데?' 이렇게 되는 경우가 실제로 많다.

### 5.2 즉시 발행

```java
// 즉시 발행이 필요한 경우 (트랜잭션과 무관한 이벤트)
@Service  
public class NotificationService {
    
    public void sendPushNotification(Long userId, String message) {
        PushNotificationEvent event = new PushNotificationEvent(userId, message);
        kafkaTemplate.send("push-notifications", event);
    }
}
```

### 5.3 비동기 발행

```java
// 비동기 처리가 필요한 경우
@Service
public class AnalyticsService {
    
    @Async
    public void trackUserActivity(UserActivityEvent event) {
        kafkaTemplate.send("user-activity", event);
    }
}
```

---

## 6. 메시지 신뢰성 보장

### 6.1 Producer 설정

```yaml
kafka:
  producer:
    properties:
      enable.idempotence: true # 중복 메시지 방지
      retries: 3 # 재시도 횟수
      acks: all # 모든 복제본 확인 후 응답
```

### 6.2 Consumer 설정

```yaml
kafka:
  consumer:
    properties:
      auto.offset.reset: earliest # 파티션 증설 시 메시지 누락 방지
      enable.auto.commit: true # 자동 커밋 활성화
      auto.commit.interval.ms: 1000 # 1초마다 커밋
```

---

## 7. 토픽 설계 전략

### 7.1 도메인별 토픽 분리

```java
private String generateTopicName(String eventType) {
    return switch (eventType) {
        case "ORDER_COMPLETED" -> "order-completed";
        case "COUPON_ISSUED" -> "coupon-issue";
        case "USER_ACTIVITY" -> "user-activity";
        case "BALANCE_CHARGED" -> "balance-activity";
        case "PRODUCT_STOCK_CHANGED" -> "product-activity";
        default -> "general-events";
    };
}
```

### 7.2 메타데이터 포함

```java
Message<DomainEvent> message = MessageBuilder
    .withPayload(event)
    .setHeader(KafkaHeaders.TOPIC, topicName)
    .setHeader(KafkaHeaders.KEY, partitionKey)
    .setHeader("eventId", event.getEventId())
    .setHeader("eventType", event.getEventType())
    .setHeader("aggregateId", event.getAggregateId())
    .setHeader("occurredOn", event.getOccurredAt().toString())
    .build();
```

---

## 8. Consumer Group 활용

### 8.1 로드밸런싱

- 동일한 Consumer Group 내 여러 Consumer가 파티션을 나누어 처리
- Consumer 수 ≤ Partition 수 (권장)

### 8.2 장애 복구

- Consumer 장애 시 자동으로 파티션 재배정 (Rebalancing)
- Offset 커밋을 통한 처리 위치 추적

---

## 9. 실제 적용 사례

### 9.1 주문 완료 이벤트 발행

```java
// OrderService에서 주문 완료 후
DomainEvent event = new OrderCompletedEvent(order.getId(), order.getUserId(), order.getFinalAmount());
eventPublisher.publishEventAfterCommit(event);
```

### 9.2 데이터 플랫폼 연동

```java
// 외부 데이터 플랫폼이 주문 데이터 구독
@KafkaListener(topics = "order-completed", groupId = "data-platform-consumer-group")
public void handleOrderCompleted(@Payload OrderDataPlatformEvent event) {
    // 분석 데이터베이스에 저장, 대시보드 업데이트 등
}
```

---

## 10. 성능 최적화 고려사항

### 10.1 배치 처리

```yaml
fetch.max.wait.ms: 500 # fetch 대기 시간
max.poll.records: 500 # 한 번에 poll할 최대 레코드 수
```

### 10.2 메모리 관리

```yaml
fetch.max.bytes: 52428800 # 최대 fetch 크기 (50MB)
```

## 11. 모니터링 및 운영

### 11.1 Kafka UI

- `http://localhost:8082`에서 토픽, 파티션, Consumer Group 모니터링
- 메시지 확인 및 Offset 관리

### 11.2 로그 기반 디버깅

```java
log.info("📡 트랜잭션 커밋 완료 후 Kafka 발행: type={}, eventId={}",
         event.getEventType(), event.getEventId());
log.info("✅ Kafka 발행 성공: topic={}, key={}, eventId={}",
         topicName, partitionKey, event.getEventId());
```

## 결론

Kafka는 **이벤트 기반 아키텍처**의 핵심 인프라로서:

- 서비스 간 느슨한 결합 제공
- 비동기 처리를 통한 성능 향상
- 확장 가능한 메시징 플랫폼 구축

본 프로젝트에서는 **트랜잭션 커밋 후 이벤트 발행** 패턴을 통해 데이터 일관성을 보장하면서도 이벤트 기반 아키텍처의 장점을 활용하고 있습니다.
