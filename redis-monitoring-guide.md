# Redis Insight 실시간 모니터링 가이드

## 연결 설정
- **Host**: 127.0.0.1
- **Port**: 7001 (마스터 노드)
- **Database Name**: E-commerce Local Cluster
- **Connection Type**: Redis Cluster

## 모니터링할 키 패턴

### 1. 캐시 관련 키
```
products::*                    # Spring Cache로 생성되는 상품 캐시
ecommerce::product::*          # 수동 생성 상품 캐시
popular-products::*            # 인기 상품 캐시
```

### 2. 분산락 관련 키
```
ecommerce:lock:*               # 모든 분산락
ecommerce:lock:order:*         # 주문 처리 락
ecommerce:lock:product:*       # 상품 관련 락
ecommerce:lock:coupon:*        # 쿠폰 관련 락
```

### 3. 통계 데이터 키
```
ecommerce:metrics:*            # 성능 메트릭
ecommerce:stats:*              # 통계 데이터
```

## Redis Insight에서 확인할 내용

### Browser 탭
1. **키 패턴 검색**: `products::*` 입력하여 캐시 상태 확인
2. **TTL 모니터링**: 각 키의 만료 시간 실시간 확인
3. **메모리 사용량**: 키별 메모리 사용량 확인

### Profiler 탭
1. **실시간 명령어 모니터링**
2. **SET/GET/DEL 명령어 패턴 분석**
3. **분산락 획득/해제 과정 추적**

### Memory Analysis 탭
1. **메모리 사용량 패턴 분석**
2. **큰 키들 식별**
3. **메모리 누수 가능성 체크**

## 성능 테스트 중 확인사항

### 캐시 동작 확인
1. **캐시 생성**: 첫 조회 시 `products::product::1` 키 생성
2. **캐시 히트**: 재조회 시 GET 명령어만 실행
3. **캐시 무효화**: 업데이트 시 DEL 명령어 실행

### 분산락 동작 확인
1. **락 획득**: `ecommerce:lock:order:process:1` 키 SET
2. **락 유지**: TTL 카운트다운 모니터링
3. **락 해제**: DEL 명령어로 키 삭제

## 실시간 API를 통한 모니터링

### 터미널에서 실시간 확인
```bash
# 캐시 상태 확인
curl http://localhost:8080/api/monitoring/redis/cache

# 락 상태 확인  
curl http://localhost:8080/api/monitoring/redis/locks

# 통계 확인
curl http://localhost:8080/api/monitoring/redis/stats
```

### 연속 모니터링
```bash
# 1초마다 캐시 상태 확인
watch -n 1 'curl -s http://localhost:8080/api/monitoring/redis/cache | jq .'

# 1초마다 락 상태 확인
watch -n 1 'curl -s http://localhost:8080/api/monitoring/redis/locks | jq .'
```