# 테스트 전략 로드맵

## 🎯 현재 상황
- **Improved 테스트**: 4개 (100% 통과)
- **기존 테스트**: 23개 (다수 실패 예상)
- **전략**: 비즈니스 중심 테스트 우선 채택

## 🚀 Phase별 실행 계획

### Phase 1: Improved 테스트 완성 (우선순위)

#### 1주차: 핵심 도메인 커버리지 완성
- [ ] `ImprovedBalanceBusinessTest` 생성
  - 잔액 충전 비즈니스 시나리오
  - 동시성 충돌 상황 처리
  - 한도 제한 검증

- [ ] `ImprovedCouponBusinessTest` 생성  
  - 쿠폰 발급 공정성
  - 선착순 로직 검증
  - 중복 발급 방지

#### 2주차: 통합 시나리오 테스트
- [ ] `ImprovedE2EBusinessTest` 생성
  - 전체 주문 플로우 (잔액충전 → 쿠폰발급 → 주문결제)
  - 실제 사용자 시나리오

### Phase 2: 기존 테스트 정리 (선택적)

#### 즉시 삭제 (가치 없음)
```bash
rm src/test/java/**/CompatibilityFixedTestContainersTest.java
rm src/test/java/**/MinimalTestContainersTest.java
rm src/test/java/**/SimpleIntegrationTest.java
rm src/test/java/**/TestContainersDebugTest.java # 중복 제거
```

#### 선택적 보완 (특수 목적)
- **ConcurrencyIntegrationTest**: 동시성 전용 테스트로 유지
- **DistributedLockIntegrationTest**: Redis 락 검증용 유지
- **ApplicationVerificationTest**: 헬스체크 전용 유지

#### 점진적 마이그레이션 
- 기존 단위 테스트들은 **Improved 테스트로 기능이 대체**되면 제거
- 비즈니스 가치가 있는 테스트 케이스만 Improved로 이전

### Phase 3: 운영 효율성 (장기)

#### CI/CD 최적화
```yaml
# 빠른 피드백 루프
- name: Quick Test (Improved만)
  run: ./gradlew test --tests "Improved*Test"
  
# 전체 검증 (선택적)  
- name: Full Test (필요시)
  run: ./gradlew test --tests "*IntegrationTest"
```

## 📊 성공 지표

### 단기 목표 (1-2주)
- [x] Improved 테스트 4개 100% 통과 ✅
- [ ] 추가 Improved 테스트 2개 생성
- [ ] 불필요한 테스트 5개 제거

### 장기 목표 (1-2개월)
- [ ] Improved 테스트가 **비즈니스 로직 90% 커버**
- [ ] 전체 테스트 실행시간 **50% 단축**
- [ ] 테스트 실패율 **5% 이하** 달성

## 💡 원칙

1. **비즈니스 가치 우선**: 구현이 아닌 비즈니스 행동 검증
2. **실용적 접근**: 완벽보다는 점진적 개선
3. **투자 대비 효과**: ROI가 높은 테스트 우선
4. **유지보수성**: 리팩토링에 강한 테스트

## ⚡ 즉시 실행 항목

### 오늘 할 일
1. 불필요한 테스트 4개 삭제
2. ImprovedBalanceBusinessTest 스케치 작성
3. 기존 BalanceServiceTest에서 비즈니스 시나리오 추출

### 이번 주 목표  
- Improved 테스트 커버리지 **60% → 85%**로 향상
- 테스트 실행 속도 **30% 개선**