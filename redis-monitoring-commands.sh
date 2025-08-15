#!/bin/bash

# Redis Insight 모니터링을 위한 유용한 명령어들

echo "=== Redis 클러스터 상태 확인 ==="
docker exec redis-7001 redis-cli -p 7001 cluster nodes

echo -e "\n=== 클러스터 정보 ==="
docker exec redis-7001 redis-cli -p 7001 cluster info

echo -e "\n=== 전체 키 개수 (각 노드별) ==="
for port in 7001 7002 7003 7004 7005 7006; do
    echo "Node $port:"
    docker exec redis-7001 redis-cli -p $port dbsize
done

echo -e "\n=== 캐시 키 패턴별 조회 ==="
echo "상품 캐시:"
docker exec redis-7001 redis-cli -p 7001 --scan --pattern "products::*"

echo -e "\n분산락 키:"
docker exec redis-7001 redis-cli -p 7001 --scan --pattern "ecommerce:lock:*"

echo -e "\n=== 애플리케이션 모니터링 API ==="
echo "활성 락 조회: curl http://localhost:8080/api/monitoring/redis/locks"
echo "캐시 상태 조회: curl http://localhost:8080/api/monitoring/redis/cache"
echo "Redis 통계: curl http://localhost:8080/api/monitoring/redis/stats"
echo "테스트 데이터 생성: curl http://localhost:8080/api/monitoring/redis/test-data"
echo "테스트 데이터 정리: curl http://localhost:8080/api/monitoring/redis/cleanup"

echo -e "\n=== 실시간 모니터링 (매 3초) ==="
echo "watch -n 3 'curl -s http://localhost:8080/api/monitoring/redis/stats | jq .'"