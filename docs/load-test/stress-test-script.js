import http from "k6/http";
import { check } from "k6";
import { Rate, Counter } from "k6/metrics";

// 커스텀 메트릭
export const errorRate = new Rate("errors");
export const systemFailures = new Counter("system_failures");

// Stress Test 설정 - 점진적으로 부하 증가하여 한계점 탐색
export const options = {
  stages: [
    { duration: "2m", target: 100 },   // 기본 부하
    { duration: "3m", target: 300 },   // 중간 부하
    { duration: "3m", target: 500 },   // 높은 부하
    { duration: "2m", target: 800 },   // 임계 부하 (한계점 탐색)
    { duration: "5m", target: 1000 },  // 최대 부하 (시스템 한계 측정)
    { duration: "2m", target: 0 },     // 점진적 감소
  ],
  thresholds: {
    // 관대한 임계값 설정 (한계점 측정이 목적)
    http_req_duration: ["p(95)<10000"], // 95%가 10초 이내
    errors: ["rate<0.5"], // 에러율 50% 미만 (한계 상황 고려)
    
    // 개별 API 한계점 측정
    "http_req_duration{name:products_list}": ["p(95)<5000"],
    "http_req_duration{name:create_order}": ["p(95)<15000"],
    "http_req_duration{name:coupon_issue}": ["p(95)<10000"],
  },
};

const BASE_URL = "http://localhost:8080";

export function setup() {
  console.log("🔥 STRESS TEST 시작 - 시스템 한계점 탐색");
  
  // 스트레스 테스트용 더 많은 데이터
  return {
    userIds: Array.from({ length: 2000 }, (_, i) => i + 1),
    productIds: Array.from({ length: 200 }, (_, i) => i + 1),
    couponIds: [1, 2, 3, 4, 5],
  };
}

export default function (data) {
  const userId = data.userIds[Math.floor(Math.random() * data.userIds.length)];
  const productId = data.productIds[Math.floor(Math.random() * data.productIds.length)];
  const couponId = data.couponIds[Math.floor(Math.random() * data.couponIds.length)];

  // 스트레스 상황에서의 사용자 행동 패턴 (더 집중적)
  const scenario = Math.random();
  
  if (scenario < 0.4) {
    // 40%: 주문 집중 시나리오
    intensiveOrderFlow(userId, productId);
  } else if (scenario < 0.2) {
    // 20%: 쿠폰 발급 집중
    intensiveCouponFlow(userId, couponId);
  } else {
    // 40%: 일반 브라우징 (시스템 부하 분산)
    basicBrowsingFlow(userId, productId);
  }
}

function intensiveOrderFlow(userId, productId) {
  // 주문 프로세스 집중 테스트
  
  // 1. 빠른 상품 조회
  const productsResponse = quickProductLookup();
  if (productsResponse.status !== 200) {
    systemFailures.add(1);
    return;
  }
  
  // 2. 잔액 확인 (필수)
  const balanceResponse = checkUserBalance(userId);
  if (balanceResponse.status !== 200) {
    systemFailures.add(1);
    return;
  }
  
  // 3. 주문 시도 (80% 확률)
  if (Math.random() < 0.8) {
    const orderResponse = attemptOrder(userId, productId);
    
    // 4. 주문 성공 시 즉시 조회 (부하 증대)
    if (orderResponse && orderResponse.status === 201) {
      checkOrderStatus(userId);
    }
  }
}

function intensiveCouponFlow(userId, couponId) {
  // 쿠폰 시스템 집중 부하 테스트
  
  // 1. 쿠폰 재고 확인
  checkCouponStock(couponId);
  
  // 2. 쿠폰 발급 시도
  const issueResponse = issueCouponAggressive(userId, couponId);
  
  // 3. 발급 상태 집중 확인 (시스템 부하 증대)
  if (issueResponse && issueResponse.status === 202) {
    try {
      const body = JSON.parse(issueResponse.body);
      const requestId = body.data.requestId;
      
      // 연속적으로 상태 확인 (부하 증대)
      for (let i = 0; i < 3; i++) {
        checkCouponStatusAggressive(requestId);
      }
    } catch (e) {
      systemFailures.add(1);
    }
  }
}

function basicBrowsingFlow(userId, productId) {
  // 기본적인 브라우징 (안정성 확인)
  
  quickProductLookup();
  
  if (Math.random() < 0.3) {
    checkUserBalance(userId);
  }
}

// =============================================================================
// 스트레스 테스트용 API 함수들 (더 엄격한 검증)
// =============================================================================

function quickProductLookup() {
  const response = http.get(`${BASE_URL}/api/v1/products`, {
    tags: { name: "products_list" },
    timeout: "5s", // 타임아웃 설정
  });

  const success = check(response, {
    "상품 조회 응답": (r) => r.status !== 0, // 최소한의 응답
    "타임아웃 없음": (r) => r.timings.duration < 5000,
  });

  if (!success) {
    errorRate.add(1);
    if (response.status === 0) {
      systemFailures.add(1);
      console.log("🚨 시스템 무응답 감지 - 상품 API");
    }
  }

  return response;
}

function checkUserBalance(userId) {
  const response = http.get(`${BASE_URL}/api/v1/users/${userId}/balance`, {
    tags: { name: "user_balance" },
    timeout: "3s",
  });

  const success = check(response, {
    "잔액 조회 응답": (r) => r.status !== 0,
    "잔액 서비스 가용": (r) => r.status < 500, // 5xx 서버 에러가 아님
  });

  if (!success) {
    errorRate.add(1);
    if (response.status >= 500 || response.status === 0) {
      systemFailures.add(1);
      console.log(`🚨 시스템 장애 감지 - 잔액 API: Status=${response.status}`);
    }
  }

  return response;
}

function attemptOrder(userId, productId) {
  const orderData = {
    userId: userId,
    orderItems: [
      {
        productId: productId,
        quantity: Math.floor(Math.random() * 2) + 1, // 1-2개 (부하 경감)
      }
    ]
  };

  const response = http.post(
    `${BASE_URL}/api/v1/orders`, 
    JSON.stringify(orderData),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "create_order" },
      timeout: "10s", // 주문은 더 긴 타임아웃
    }
  );

  const success = check(response, {
    "주문 API 응답": (r) => r.status !== 0,
    "주문 처리 가능": (r) => r.status !== 503, // Service Unavailable
    "타임아웃 없음": (r) => r.timings.duration < 10000,
  });

  if (!success) {
    errorRate.add(1);
    
    // 치명적 오류 감지
    if (response.status === 0 || response.status === 503) {
      systemFailures.add(1);
      console.log(`🚨 주문 시스템 장애: Status=${response.status}, Duration=${response.timings.duration}ms`);
    }
  }

  return response;
}

function checkOrderStatus(userId) {
  const response = http.get(`${BASE_URL}/api/v1/orders/users/${userId}`, {
    tags: { name: "user_orders" },
    timeout: "5s",
  });

  const success = check(response, {
    "주문 조회 응답": (r) => r.status !== 0,
    "조회 서비스 가용": (r) => r.status < 500,
  });

  if (!success) {
    errorRate.add(1);
    if (response.status >= 500 || response.status === 0) {
      systemFailures.add(1);
    }
  }

  return response;
}

function issueCouponAggressive(userId, couponId) {
  const couponData = {
    userId: userId,
    couponId: couponId,
  };

  const response = http.post(
    `${BASE_URL}/api/coupons/async/issue`, 
    JSON.stringify(couponData),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "coupon_issue" },
      timeout: "8s",
    }
  );

  const success = check(response, {
    "쿠폰 API 응답": (r) => r.status !== 0,
    "쿠폰 시스템 가용": (r) => r.status !== 503,
    "Redis 연결 가능": (r) => r.status !== 502, // Bad Gateway (Redis 장애)
  });

  if (!success) {
    errorRate.add(1);
    
    if (response.status === 502) {
      systemFailures.add(1);
      console.log("🚨 Redis 연결 장애 감지");
    } else if (response.status === 503) {
      systemFailures.add(1);
      console.log("🚨 쿠폰 시스템 과부하");
    }
  }

  return response;
}

function checkCouponStatusAggressive(requestId) {
  const response = http.get(`${BASE_URL}/api/coupons/async/status/${requestId}`, {
    tags: { name: "coupon_status" },
    timeout: "3s",
  });

  const success = check(response, {
    "상태 조회 응답": (r) => r.status !== 0,
    "상태 서비스 가용": (r) => r.status < 500,
  });

  if (!success) {
    errorRate.add(1);
  }

  return response;
}

function checkCouponStock(couponId) {
  const response = http.get(`${BASE_URL}/api/coupons/async/${couponId}/stock`, {
    tags: { name: "coupon_stock" },
    timeout: "2s",
  });

  const success = check(response, {
    "재고 조회 응답": (r) => r.status !== 0,
    "Redis 상태 정상": (r) => r.status !== 502,
  });

  if (!success) {
    errorRate.add(1);
  }

  return response;
}

export function teardown(data) {
  console.log("🔥 STRESS TEST 완료");
  console.log("=====================================");
  console.log("📊 스트레스 테스트 결과:");
  console.log(`🚨 시스템 장애 발생 횟수: ${systemFailures.count}`);
  console.log(`👥 최대 부하: 1000 사용자`);
  console.log(`⏱️  테스트 지속 시간: 17분`);
  
  if (systemFailures.count > 100) {
    console.log("⚠️  심각한 시스템 불안정성 감지 - 즉시 분석 필요");
  } else if (systemFailures.count > 10) {
    console.log("⚠️  부분적 시스템 장애 감지 - 성능 최적화 권장");
  } else {
    console.log("✅ 시스템이 고부하 상황에서 안정적으로 동작");
  }
  
  console.log("=====================================");
}