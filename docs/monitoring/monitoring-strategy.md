# 모니터링 전략 및 개선 방안

## 🎯 모니터링 철학

### 핵심 원칙
```yaml
예방 중심의 관찰 가능성:
- 장애 발생 후 분석이 아닌 발생 전 징후 감지
- 사용자 경험 중심의 메트릭 설계  
- 자동화된 대응 및 복구 메커니즘
- 지속적인 학습과 개선
```

### 모니터링 목표
1. **사용자 영향 최소화**: MTTD < 1분, MTTR < 5분
2. **예방적 감지**: 장애 발생 전 3-5분 사전 알림
3. **자동 복구**: 일반적 문제의 80% 자동 해결
4. **지속적 개선**: 매주 임계값 및 정책 최적화

## 📊 3계층 모니터링 아키텍처

### Layer 1: 비즈니스 메트릭 (Business KPI)
```yaml
목적: 사용자 경험 및 비즈니스 임팩트 측정
주기: 실시간 (1분 간격)

핵심 지표:
  사용자 경험:
    - 페이지 로딩 시간 (P95 < 2초)
    - API 응답 시간 (P99 < 5초) 
    - 에러율 (< 0.1%)
    - 사용자 세션 성공률 (> 99%)

  비즈니스 임팩트:
    - 주문 완료율 (> 95%)
    - 쿠폰 발급 성공률 (> 98%)
    - 결제 성공률 (> 99.5%)
    - 일일 활성 사용자 수 (DAU)
```

### Layer 2: 애플리케이션 메트릭 (Application Health)
```yaml
목적: 서비스 상태 및 성능 추적
주기: 30초 간격

성능 지표:
  처리량:
    - TPS (Transactions Per Second)
    - API별 요청 수 (RPM)
    - 동시 접속자 수
    - Queue 처리량 (Kafka)

  응답성:
    - API별 응답시간 분포 (P50/P95/P99)
    - DB 쿼리 응답시간
    - 외부 API 호출 응답시간
    - 캐시 히트율

  안정성:
    - HTTP 상태 코드 분포
    - 예외 발생 건수 및 유형
    - 재시도 횟수
    - Circuit Breaker 상태
```

### Layer 3: 인프라 메트릭 (Infrastructure)
```yaml
목적: 시스템 리소스 및 의존성 모니터링
주기: 15초 간격

시스템 리소스:
  컴퓨팅:
    - CPU 사용률 (< 70%)
    - 메모리 사용률 (< 80%)
    - JVM 힙 사용률 (< 75%)
    - GC 빈도 및 지속시간

  스토리지:
    - 디스크 사용률 (< 85%)
    - 디스크 I/O 대기시간
    - 파일 시스템 상태

  네트워크:
    - 네트워크 I/O 처리량
    - 연결 상태 (established/waiting)
    - 패킷 손실률

의존성 서비스:
  데이터베이스:
    - 커넥션 풀 사용률 (< 70%)
    - 슬로우 쿼리 발생률
    - Lock 대기 시간
    - 복제 지연시간

  캐시 시스템:
    - Redis 메모리 사용률 (< 80%) 
    - 연결 풀 상태
    - 키 만료율
    - 클러스터 노드 상태

  메시지 큐:
    - Kafka Consumer Lag
    - Topic별 메시지 처리율
    - Broker 상태
    - Partition 분산 상태
```

## 🚨 알림 및 에스컬레이션 정책

### 알림 심각도 분류

#### 🔴 Critical (즉시 대응 필요)
```yaml
조건:
  - 전체 서비스 가용성 < 95%
  - API 에러율 > 10%
  - DB 커넥션 풀 사용률 > 90%
  - P95 응답시간 > 5초
  - 메모리 사용률 > 95%

대응:
  - 즉시 Slack/SMS로 온콜 담당자에게 알림
  - 5분 내 미대응 시 팀 리드에게 에스컬레이션
  - 15분 내 미해결 시 전체 개발팀 호출
  - 30분 내 미해결 시 경영진 보고
```

#### 🟡 Warning (주의 필요)
```yaml  
조건:
  - CPU 사용률 > 70%
  - DB 커넥션 풀 사용률 > 70%
  - P95 응답시간 > 2초  
  - 에러율 > 1%
  - 슬로우 쿼리 발생 (>3초)

대응:
  - Slack 채널에 알림
  - 30분 지속 시 담당자 태그
  - 1시간 지속 시 Critical로 승격
```

#### 🔵 Info (정보성)
```yaml
조건:
  - 배포 완료
  - 자동 복구 성공
  - 임계값 근접 (90% 도달)
  - 성능 개선 감지

대응:
  - Slack 채널에 정보성 메시지
  - 일일 리포트에 포함
```

### 알림 채널 및 담당자

```yaml
알림 채널:
  Slack:
    - #alerts-critical (즉시 알림)
    - #alerts-warning (주의 알림)  
    - #monitoring-info (정보성)

  SMS/전화:
    - Critical 알림 시 온콜 담당자
    - 1차: 서버 개발자 (24시간)
    - 2차: DevOps 엔지니어 (평일)
    - 3차: 팀 리드 (항상)

온콜 로테이션:
  - 주간 교대제 (월-일)
  - 휴일/연휴 특별 대응팀 운영
  - 해외 출장 시 백업 담당자 지정
```

## 📈 대시보드 설계

### Executive Dashboard (경영진용)
```yaml
목적: 비즈니스 KPI 중심의 고수준 현황
업데이트: 5분 간격

주요 위젯:
  - 서비스 가용성 (SLA 달성률)
  - 일일 매출 및 주요 지표
  - 장애 발생 현황 및 복구 상태
  - 사용자 만족도 점수
  - 월간/분기 트렌드 분석
```

### Operations Dashboard (운영팀용)  
```yaml
목적: 실시간 서비스 상태 모니터링
업데이트: 실시간 (30초)

주요 위젯:
  - 전체 서비스 상태 맵
  - API별 응답시간 및 에러율
  - 인프라 리소스 사용률
  - 활성 알림 및 처리 상태
  - 최근 배포 및 변경 이력
```

### Engineering Dashboard (개발팀용)
```yaml
목적: 기술적 세부 사항 및 성능 분석
업데이트: 실시간 (15초)

주요 위젯:
  - JVM 메모리 및 GC 상태
  - DB 쿼리 성능 분석
  - 캐시 성능 및 히트율
  - Kafka Consumer Lag
  - 에러 로그 실시간 스트림
  - 코드 배포 파이프라인 상태
```

### Business Intelligence Dashboard (분석팀용)
```yaml
목적: 사용자 행동 및 비즈니스 트렌드 분석
업데이트: 1시간 간격

주요 위젯:
  - 사용자 플로우 분석
  - 쿠폰/이벤트 성과 분석
  - 매출 기여도 분석
  - 성능이 비즈니스에 미치는 영향
  - 예측 및 추천 인사이트
```

## 🛠️ 모니터링 도구 스택

### 메트릭 수집 및 저장
```yaml
현재 (단순한 구성):
  - Spring Boot Actuator (메트릭 노출)
  - Micrometer (메트릭 수집)
  - 로컬 로그 파일

권장 구성 (확장된 구성):
  - Prometheus (메트릭 수집 및 저장)
  - Grafana (시각화 및 대시보드)
  - AlertManager (알림 관리)
```

### 로그 관리
```yaml
현재:
  - 로컬 파일 로깅
  - 수동 로그 분석

권장 구성:
  수집: Filebeat/Fluentd
  전송: Logstash
  저장: Elasticsearch  
  시각화: Kibana
  분석: ElasticSearch Query
```

### 분산 추적
```yaml
현재:
  - 없음 (단일 애플리케이션)

권장 구성:
  - Jaeger/Zipkin (분산 트레이싱)
  - OpenTelemetry (계측)
  - 요청 플로우 시각화
```

### 합성 모니터링
```yaml
목적: 사용자 관점에서 서비스 가용성 확인

도구:
  - Postman Newman (API 테스트)
  - Playwright (E2E 테스트)
  - K6 (정기 부하 테스트)

시나리오:
  - 핵심 사용자 플로우 (로그인 → 상품조회 → 주문)
  - API 응답 시간 및 정확성
  - 외부 의존성 연결성
```

## 🎯 즉시 적용 가능한 개선 방안

### 1단계: 기본 모니터링 강화 (1주 내)

**Spring Boot Actuator 활성화**
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

**커스텀 메트릭 추가**
```java
@Component
public class BusinessMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter orderSuccessCounter;
    private final Timer couponIssueTimer;
    
    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.orderSuccessCounter = Counter.builder("orders.success")
            .description("Successful order count")
            .register(meterRegistry);
        this.couponIssueTimer = Timer.builder("coupon.issue.duration")
            .description("Coupon issue processing time")
            .register(meterRegistry);
    }
    
    public void recordOrderSuccess() {
        orderSuccessCounter.increment();
    }
    
    public void recordCouponIssueTime(Duration duration) {
        couponIssueTimer.record(duration);
    }
}
```

**헬스 체크 강화**
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "Available")
                    .withDetail("connection_pool", getPoolStatus())
                    .build();
            }
        } catch (SQLException e) {
            return Health.down(e)
                .withDetail("database", "Unavailable")
                .build();
        }
        return Health.down()
            .withDetail("database", "Unknown")
            .build();
    }
}
```

### 2단계: 알림 시스템 구축 (2주 내)

**Slack 웹훅 설정**
```java
@Service
public class AlertService {
    
    @Value("${alert.slack.webhook}")
    private String slackWebhook;
    
    public void sendCriticalAlert(String message) {
        SlackMessage alert = SlackMessage.builder()
            .channel("#alerts-critical")
            .text("🚨 CRITICAL: " + message)
            .username("Monitoring Bot")
            .build();
            
        webClient.post()
            .uri(slackWebhook)  
            .bodyValue(alert)
            .retrieve()
            .bodyToMono(String.class)
            .subscribe();
    }
}
```

**자동 알림 트리거**
```java
@Component
public class HealthMonitor {
    
    @Scheduled(fixedRate = 30000) // 30초마다 체크
    public void checkSystemHealth() {
        SystemHealth health = systemHealthService.getCurrentHealth();
        
        if (health.getCpuUsage() > 70) {
            alertService.sendWarningAlert(
                "CPU usage high: " + health.getCpuUsage() + "%"
            );
        }
        
        if (health.getDatabaseConnectionUsage() > 70) {
            alertService.sendWarningAlert(
                "DB connection pool usage: " + health.getDatabaseConnectionUsage() + "%"
            );
        }
    }
}
```

### 3단계: 대시보드 구성 (1개월 내)

**Grafana 대시보드 JSON 설정**
```json
{
  "dashboard": {
    "title": "E-Commerce Service Overview",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      },
      {
        "title": "Response Time",
        "type": "graph", 
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          }
        ]
      }
    ]
  }
}
```

## 📋 모니터링 메트릭 상세 정의

### 핵심 SLI (Service Level Indicators)

```yaml
가용성 SLI:
  정의: 성공적인 요청 / 전체 요청
  목표: > 99.9% (월 기준)
  측정: HTTP 2xx 응답 / 전체 HTTP 응답

응답성 SLI:  
  정의: P95 응답시간
  목표: < 2초 (API별)
  측정: 요청 시작부터 응답 완료까지

정확성 SLI:
  정의: 논리적으로 정확한 응답
  목표: > 99.99%
  측정: 비즈니스 로직 검증 통과율
```

### API별 성능 목표

```yaml
상품 조회 API:
  P95 응답시간: < 500ms
  P99 응답시간: < 1초
  에러율: < 0.1%
  TPS: > 500

주문 생성 API:
  P95 응답시간: < 2초
  P99 응답시간: < 5초  
  에러율: < 1%
  TPS: > 100

쿠폰 발급 API:
  P95 응답시간: < 1초
  P99 응답시간: < 3초
  에러율: < 2% (선착순 특성상)
  TPS: > 200
```

## 🚀 자동화 및 자가 치유

### 자동 복구 시나리오

```java
@Component
public class AutoRecoveryService {
    
    // DB 커넥션 풀 자동 확장
    @EventListener
    public void handleHighConnectionUsage(HighConnectionUsageEvent event) {
        if (event.getUsagePercentage() > 80) {
            databaseService.expandConnectionPool(
                currentSize * 1.5
            );
            log.info("자동으로 DB 커넥션 풀 확장: {}", currentSize * 1.5);
        }
    }
    
    // 메모리 부족 시 캐시 정리
    @EventListener  
    public void handleHighMemoryUsage(HighMemoryUsageEvent event) {
        if (event.getUsagePercentage() > 85) {
            cacheService.evictLeastRecentlyUsed();
            System.gc(); // 명시적 GC 호출 (주의해서 사용)
            log.info("메모리 부족으로 캐시 정리 및 GC 실행");
        }
    }
    
    // Circuit Breaker 자동 복구 시도
    @Scheduled(fixedRate = 60000) // 1분마다
    public void tryCircuitBreakerRecovery() {
        circuitBreakerRegistry.getAllCircuitBreakers()
            .forEach(cb -> {
                if (cb.getState() == CircuitBreaker.State.OPEN) {
                    // Health check를 통한 복구 시도
                    if (healthCheckService.isHealthy(cb.getName())) {
                        cb.transitionToHalfOpenState();
                    }
                }
            });
    }
}
```

### 예측 기반 스케일링

```java
@Service
public class PredictiveScalingService {
    
    // 과거 패턴 기반 리소스 예측
    public void predictAndScale() {
        TrendAnalysis trend = metricsAnalyzer.analyzePastWeek();
        
        // 트래픽 급증이 예상되는 시간대
        if (trend.isPeakTimeApproaching()) {
            // 사전에 리소스 확장
            resourceManager.preemptiveScale(
                trend.getPredictedPeakLoad()
            );
            
            alertService.sendInfoAlert(
                "예상 트래픽 급증에 따른 사전 스케일링 실행"
            );
        }
    }
}
```

## 💡 모니터링 성숙도 로드맵

### Level 1: 반응형 (현재 상태)
- 장애 발생 후 수동 대응
- 기본적인 로그 및 에러 추적
- 단순한 헬스 체크

### Level 2: 예방형 (1-2개월 목표)
- 임계값 기반 자동 알림
- 대시보드를 통한 시각화
- 기본적인 메트릭 수집

### Level 3: 예측형 (3-6개월 목표)  
- 머신러닝 기반 이상 탐지
- 자동 스케일링 및 복구
- 비즈니스 영향도 분석

### Level 4: 자율형 (6-12개월 목표)
- 완전 자동화된 운영
- 자가 치유 시스템
- 지능형 용량 계획

## 📊 성공 지표 및 KPI

### 모니터링 효과성 측정
```yaml
기술 지표:
  - MTTD (Mean Time To Detection): < 1분
  - MTTR (Mean Time To Recovery): < 5분  
  - 장애 예방율: > 80% (사전 감지 및 대응)
  - False Positive 비율: < 5%

비즈니스 지표:
  - 서비스 가용성: > 99.9%
  - 사용자 만족도: > 4.5/5.0
  - 장애로 인한 매출 손실: < 월매출의 0.1%
  - 운영 비용 절감: > 30%
```

### 정기 평가 및 개선
```yaml
일일 평가:
  - 알림 정확도 검토
  - 임계값 적정성 확인
  - 대시보드 유용성 피드백

주간 평가:
  - 트렌드 분석 및 패턴 파악
  - 새로운 메트릭 필요성 검토
  - 자동화 개선 기회 식별

월간 평가:
  - 전체 모니터링 전략 검토
  - ROI 분석 및 비용 최적화
  - 다음 달 개선 계획 수립
```

---

**이 모니터링 전략을 통해 달성하고자 하는 최종 목표:**
> "장애가 사용자에게 영향을 주기 전에 미리 감지하고 자동으로 해결하는 시스템"

모니터링은 단순한 도구가 아니라 **서비스 안정성을 보장하는 핵심 전략**입니다. 지속적인 개선과 학습을 통해 더욱 견고하고 신뢰할 수 있는 시스템을 구축해 나가겠습니다.