#!/bin/bash

echo "=== Docker 환경 확인 ==="

# 1. Docker 실행 상태 확인
echo "1. Docker 데몬 상태:"
docker info > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Docker 데몬이 실행 중입니다"
else
    echo "❌ Docker 데몬이 실행되지 않습니다"
    echo "Docker Desktop을 시작하거나 'sudo systemctl start docker' 실행"
    exit 1
fi

# 2. Docker 버전 확인
echo -e "\n2. Docker 버전:"
docker --version
docker-compose --version

# 3. MySQL 이미지 확인/다운로드
echo -e "\n3. MySQL 이미지 확인:"
if docker images | grep -q "mysql.*8.0"; then
    echo "✅ MySQL 8.0 이미지가 존재합니다"
else
    echo "📥 MySQL 8.0 이미지를 다운로드합니다..."
    docker pull mysql:8.0
fi

# 4. 실행 중인 컨테이너 확인
echo -e "\n4. 실행 중인 컨테이너:"
docker ps

# 5. 네트워크 상태 확인
echo -e "\n5. Docker 네트워크:"
docker network ls

# 6. TestContainers 환경 변수 설정
echo -e "\n6. TestContainers 환경 변수 설정:"
export TESTCONTAINERS_RYUK_DISABLED=true
export TESTCONTAINERS_CHECKS_DISABLE=true
echo "✅ TestContainers 환경 변수 설정 완료"

# 7. 간단한 MySQL 컨테이너 테스트
echo -e "\n7. MySQL 컨테이너 테스트:"
docker run --rm --name mysql-test -e MYSQL_ROOT_PASSWORD=test -e MYSQL_DATABASE=testdb -d mysql:8.0
sleep 10

if docker ps | grep -q "mysql-test"; then
    echo "✅ MySQL 컨테이너 실행 성공"
    docker stop mysql-test
else
    echo "❌ MySQL 컨테이너 실행 실패"
fi

echo -e "\n=== 테스트 실행 권장 명령어 ==="
echo "export TESTCONTAINERS_RYUK_DISABLED=true"
echo "export TESTCONTAINERS_CHECKS_DISABLE=true"
echo "./gradlew clean test --info"