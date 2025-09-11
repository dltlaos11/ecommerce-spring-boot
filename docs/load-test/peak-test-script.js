import http from "k6/http";
import { check } from "k6";
import { Rate, Counter, Trend } from "k6/metrics";

// Peak Test용 커스텀 메트릭
export const errorRate = new Rate("errors");
export const couponSuccessRate = new Rate("coupon_success");
export const peakResponseTime = new Trend("peak_response_time");
export const queueOverflow = new Counter("queue_overflow");

// Peak Test 설정 - 급격한 트래픽 증가 (선착순 이벤트 시뮬레이션)
export const options = {
  stages: [
    { duration: "10s", target: 1000 },  // 급속 증가 (10초만에 1000명)
    { duration: "1m", target: 1000 },   // 피크 유지 (선착순 경쟁 시간)
    { duration: "10s", target: 0 },     // 급속 감소
  ],
  thresholds: {
    // Peak 상황에서의 기대치 (더 관대한 설정)
    http_req_duration: ["p(95)<5000"],  // 피크 시간 고려
    errors: ["rate<0.3"], // 30% 미만 (경쟁 상황 고려)
    
    // 쿠폰 관련 핵심 지표
    "http_req_duration{name:coupon_issue}": ["p(95)<3000"],
    coupon_success: ["rate>0.1"], // 최소 10% 성공률 (선착순 특성)
  },
  
  // 메모리 사용량 제한 (피크 테스트 안정성)
  setupTimeout: "60s",
  teardownTimeout: "60s",
};

const BASE_URL = "http://localhost:8080";

export function setup() {
  console.log("⚡ PEAK TEST 시작 - 선착순 이벤트 시뮬레이션");
  console.log("🎯 시나리오: 1분간 1000명이 동시에 쿠폰 발급 요청");
  
  // 쿠폰 재고 초기화 (테스트 시작 전)
  const initResponse = http.post(`${BASE_URL}/api/coupons/async/1/initialize-stock`);
  console.log(`🔄 쿠폰 재고 초기화: Status=${initResponse.status}`);
  
  return {
    // Peak 테스트용 제한된 데이터 (집중도 증가)
    userIds: Array.from({ length: 1500 }, (_, i) => i + 1),
    productIds: [1, 2, 3, 4, 5], // 인기 상품만
    mainCouponId: 1, // 주요 이벤트 쿠폰
    secondaryCoupons: [2, 3], // 보조 쿠폰들
    eventStartTime: Date.now(),
  };
}

export default function (data) {
  const userId = data.userIds[Math.floor(Math.random() * data.userIds.length)];
  const productId = data.productIds[Math.floor(Math.random() * data.productIds.length)];
  
  // 현재 시간 기준 이벤트 단계 결정
  const currentTime = Date.now();
  const elapsedSeconds = (currentTime - data.eventStartTime) / 1000;
  
  if (elapsedSeconds < 70) { // 처음 70초 (급증 + 피크)
    // 90% 쿠폰 집중, 10% 일반 트래픽
    if (Math.random() < 0.9) {
      couponRushScenario(userId, data.mainCouponId, productId);
    } else {
      supportTrafficScenario(userId, productId);
    }
  } else {
    // 이벤트 종료 후 일반 트래픽
    postEventScenario(userId, productId, data.secondaryCoupons);
  }
}

// 쿠폰 러시 시나리오 (90% - 메인 시나리오)
function couponRushScenario(userId, couponId, productId) {
  const startTime = Date.now();
  
  // 1. 쿠폰 발급 시도 (최우선)
  const couponResponse = rushCouponIssue(userId, couponId);
  
  // 2. 발급 성공 시 즉시 구매 시도 (50% 확률)
  if (couponResponse && couponResponse.status === 202 && Math.random() < 0.5) {
    quickPurchaseAttempt(userId, productId, couponId);
  }
  
  // 3. 실시간 상태 확인 (불안감으로 인한 반복 확인)
  if (couponResponse && couponResponse.status === 202) {
    try {
      const body = JSON.parse(couponResponse.body);
      const requestId = body.data.requestId;
      
      // 짧은 간격으로 여러 번 확인 (사용자 불안감 시뮬레이션)
      for (let i = 0; i < 2; i++) {
        setTimeout(() => {
          checkCouponStatusUrgent(requestId);
        }, (i + 1) * 500);
      }
    } catch (e) {
      console.log("쿠폰 상태 확인 실패:", e.message);
    }
  }
  
  // 응답 시간 추적
  const totalTime = Date.now() - startTime;
  peakResponseTime.add(totalTime);
}

// 지원 트래픽 시나리오 (10% - 일반적인 쇼핑)
function supportTrafficScenario(userId, productId) {
  // 이벤트 중에도 일반적인 쇼핑은 계속됨
  
  // 1. 상품 조회
  quickProductCheck();
  
  // 2. 잔액 확인 (30% 확률)
  if (Math.random() < 0.3) {
    quickBalanceCheck(userId);
  }
  
  // 3. 일반 주문 시도 (20% 확률)
  if (Math.random() < 0.2) {
    attemptNormalOrder(userId, productId);
  }
}

// 이벤트 후 시나리오
function postEventScenario(userId, productId, secondaryCoupons) {
  // 메인 이벤트 실패한 사용자들의 대안 행동
  
  // 1. 보조 쿠폰 시도 (60% 확률)
  if (Math.random() < 0.6) {
    const secondaryCoupon = secondaryCoupons[Math.floor(Math.random() * secondaryCoupons.length)];
    rushCouponIssue(userId, secondaryCoupon);
  }
  
  // 2. 일반 상품 구매로 전환 (40% 확률)
  if (Math.random() < 0.4) {
    attemptNormalOrder(userId, productId);
  }
}

// =============================================================================
// Peak Test 전용 API 함수들
// =============================================================================

function rushCouponIssue(userId, couponId) {
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
      timeout: "5s", // 짧은 타임아웃 (빠른 실패)
    }
  );

  const success = check(response, {
    "쿠폰 발급 시도": (r) => r.status !== 0,
    "서버 응답": (r) => r.status < 500,
    "빠른 응답": (r) => r.timings.duration < 3000,
  });

  // 성공률 추적
  if (response.status === 202) {
    couponSuccessRate.add(1);
  } else {
    couponSuccessRate.add(0);
    
    // 큐 오버플로우 감지
    if (response.status === 429 || response.status === 503) {
      queueOverflow.add(1);
      console.log(`⚠️ 큐 오버플로우 감지: Status=${response.status}, User=${userId}`);
    }
  }

  if (!success) {
    errorRate.add(1);
  }

  return response;
}

function checkCouponStatusUrgent(requestId) {
  const response = http.get(`${BASE_URL}/api/coupons/async/status/${requestId}`, {
    tags: { name: "coupon_status_urgent" },
    timeout: "2s", // 매우 짧은 타임아웃
  });

  const success = check(response, {
    "상태 확인 가능": (r) => r.status !== 0,
    "즉시 응답": (r) => r.timings.duration < 1000,
  });

  if (!success) {
    errorRate.add(1);
  }

  return response;
}

function quickPurchaseAttempt(userId, productId, couponId) {
  const orderData = {
    userId: userId,
    couponId: couponId, // 방금 발급받은 쿠폰 사용
    orderItems: [
      {
        productId: productId,
        quantity: 1, // 빠른 구매를 위해 1개만
      }
    ]
  };

  const response = http.post(
    `${BASE_URL}/api/v1/orders`, 
    JSON.stringify(orderData),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "quick_order_with_coupon" },
      timeout: "8s",
    }
  );

  const success = check(response, {
    "빠른 주문 시도": (r) => r.status !== 0,
    "주문 처리 중": (r) => r.status === 201 || r.status === 400, // 성공 또는 유효한 실패
  });

  if (!success) {
    errorRate.add(1);
  }

  return response;
}

function quickProductCheck() {
  const response = http.get(`${BASE_URL}/api/v1/products?onlyAvailable=true`, {
    tags: { name: "quick_product_check" },
    timeout: "2s",
  });

  const success = check(response, {
    "상품 정보 확인": (r) => r.status === 200,
    "빠른 로딩": (r) => r.timings.duration < 1000,
  });

  if (!success) {
    errorRate.add(1);
  }

  return response;
}

function quickBalanceCheck(userId) {
  const response = http.get(`${BASE_URL}/api/v1/users/${userId}/balance`, {
    tags: { name: "quick_balance_check" },
    timeout: "2s",
  });

  const success = check(response, {
    "잔액 확인": (r) => r.status === 200,
    "빠른 조회": (r) => r.timings.duration < 1000,
  });

  if (!success) {
    errorRate.add(1);
  }

  return response;
}

function attemptNormalOrder(userId, productId) {
  const orderData = {
    userId: userId,
    orderItems: [
      {
        productId: productId,
        quantity: 1,
      }
    ]
  };

  const response = http.post(
    `${BASE_URL}/api/v1/orders`, 
    JSON.stringify(orderData),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "normal_order" },
      timeout: "6s",
    }
  );

  const success = check(response, {
    "일반 주문": (r) => r.status !== 0,
    "처리 완료": (r) => r.status === 201,
  });

  if (!success) {
    errorRate.add(1);
  }

  return response;
}

export function teardown(data) {
  console.log("⚡ PEAK TEST 완료");
  console.log("=====================================");
  console.log("🎯 선착순 이벤트 시뮬레이션 결과:");
  
  // 최종 쿠폰 재고 확인
  const stockResponse = http.get(`${BASE_URL}/api/coupons/async/${data.mainCouponId}/stock`);
  let remainingStock = "확인 불가";
  
  if (stockResponse.status === 200) {
    try {
      const body = JSON.parse(stockResponse.body);
      remainingStock = body.data.currentStock;
    } catch (e) {
      console.log("재고 확인 실패:", e.message);
    }
  }
  
  console.log(`📦 이벤트 쿠폰 잔여 재고: ${remainingStock}`);
  console.log(`🎫 쿠폰 발급 성공률: ${(couponSuccessRate.rate * 100).toFixed(1)}%`);
  console.log(`⚠️  큐 오버플로우 발생: ${queueOverflow.count}회`);
  console.log(`⚡ 평균 Peak 응답시간: ${peakResponseTime.avg.toFixed(0)}ms`);
  
  // 성능 평가
  if (couponSuccessRate.rate > 0.5) {
    console.log("✅ 우수: 높은 성공률로 이벤트 처리");
  } else if (couponSuccessRate.rate > 0.2) {
    console.log("⚠️  보통: 적정 수준의 이벤트 처리");
  } else {
    console.log("❌ 개선 필요: 낮은 성공률 - 시스템 확장 고려");
  }
  
  if (queueOverflow.count < 50) {
    console.log("✅ 큐잉 시스템 안정적 동작");
  } else {
    console.log("⚠️  큐잉 시스템 확장 필요");
  }
  
  console.log("=====================================");
}