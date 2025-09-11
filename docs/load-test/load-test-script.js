import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Counter } from "k6/metrics";

// 커스텀 메트릭 정의
export const errorRate = new Rate("errors");
export const successfulTransactions = new Counter("successful_transactions");

// 테스트 설정
export const options = {
  stages: [
    { duration: "2m", target: 50 },   // 점진적 증가
    { duration: "3m", target: 100 },  // 유지
    { duration: "2m", target: 0 },    // 점진적 감소
  ],
  thresholds: {
    // 전체 응답 시간
    http_req_duration: ["p(95)<2000"], // 95%가 2초 이내
    
    // API별 응답 시간 SLA
    "http_req_duration{name:products_list}": ["p(95)<1000"],  // 상품 조회: 1초
    "http_req_duration{name:create_order}": ["p(95)<5000"],   // 주문 생성: 5초
    "http_req_duration{name:coupon_issue}": ["p(95)<3000"],   // 쿠폰 발급: 3초
    "http_req_duration{name:user_orders}": ["p(95)<2000"],    // 주문 조회: 2초
    "http_req_duration{name:user_balance}": ["p(95)<1000"],   // 잔액 조회: 1초
    
    // 에러율
    errors: ["rate<0.1"], // 전체 에러율 10% 미만
    
    // 처리량
    http_reqs: ["rate>200"], // 최소 200 TPS
  },
};

const BASE_URL = "http://localhost:8080";

// 테스트 데이터 준비
export function setup() {
  console.log("🚀 부하 테스트 시작 - 테스트 데이터 준비 중...");
  
  return {
    userIds: Array.from({ length: 1000 }, (_, i) => i + 1),
    productIds: Array.from({ length: 100 }, (_, i) => i + 1),
    couponIds: [1, 2, 3], // 테스트용 쿠폰 ID
  };
}

// 메인 테스트 함수
export default function (data) {
  const userId = data.userIds[Math.floor(Math.random() * data.userIds.length)];
  const productId = data.productIds[Math.floor(Math.random() * data.productIds.length)];
  const couponId = data.couponIds[Math.floor(Math.random() * data.couponIds.length)];

  // 사용자 유형별 시나리오 분기
  const userType = Math.random();
  
  if (userType < 0.05) {
    // 5%: 이벤트 참여 사용자 (쿠폰 발급 + 즉시 구매)
    eventParticipantFlow(userId, productId, couponId);
  } else if (userType < 0.30) {
    // 25%: 구매 의도 사용자 (검색 → 구매)
    purchaseIntentFlow(userId, productId);
  } else {
    // 70%: 일반 사용자 (브라우징만)
    generalUserFlow(userId, productId);
  }
}

// 일반 사용자 플로우 (70%)
function generalUserFlow(userId, productId) {
  // 1. 상품 목록 조회 (100%)
  browseProducts();
  
  // 2. 상품 상세 조회 (60% 확률)
  if (Math.random() < 0.6) {
    getProductDetails(productId);
  }
  
  // 3. 로그인 체크 (30% 확률)
  if (Math.random() < 0.3) {
    checkBalance(userId);
  }
}

// 구매 의도 사용자 플로우 (25%)
function purchaseIntentFlow(userId, productId) {
  // 1. 상품 검색 (100%)
  searchProducts("노트북");
  
  // 2. 상품 목록 조회
  browseProducts();
  
  // 3. 잔액 조회 (100%)
  checkBalance(userId);
  
  // 4. 주문 생성 (90% 확률)
  if (Math.random() < 0.9) {
    const orderId = createOrder(userId, productId);
    
    // 5. 주문 조회 (주문 성공 시 100%)
    if (orderId) {
      getUserOrders(userId);
      successfulTransactions.add(1);
    }
  }
}

// 이벤트 참여 사용자 플로우 (5%)
function eventParticipantFlow(userId, productId, couponId) {
  // 1. 쿠폰 발급 시도 (100%)
  issueCoupon(userId, couponId);
  
  // 2. 상품 조회
  browseProducts();
  
  // 3. 즉시 주문 시도 (70% 확률)
  if (Math.random() < 0.7) {
    const orderId = createOrderWithCoupon(userId, productId, couponId);
    if (orderId) {
      getUserOrders(userId);
      successfulTransactions.add(1);
    }
  }
}

// =============================================================================
// API 호출 함수들
// =============================================================================

function browseProducts() {
  const response = http.get(`${BASE_URL}/api/v1/products`, {
    tags: { name: "products_list" },
  });

  const success = check(response, {
    "상품 목록 조회 성공": (r) => r.status === 200,
    "상품 목록 응답시간 < 1초": (r) => r.timings.duration < 1000,
    "상품 데이터 존재": (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true && Array.isArray(body.data);
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) errorRate.add(1);
  return response;
}

function searchProducts(keyword) {
  const response = http.get(`${BASE_URL}/api/v1/products?name=${keyword}`, {
    tags: { name: "products_search" },
  });

  const success = check(response, {
    "상품 검색 성공": (r) => r.status === 200,
    "검색 응답시간 < 1초": (r) => r.timings.duration < 1000,
  });

  if (!success) errorRate.add(1);
  return response;
}

function getProductDetails(productId) {
  const response = http.get(`${BASE_URL}/api/v1/products/${productId}`, {
    tags: { name: "product_detail" },
  });

  const success = check(response, {
    "상품 상세 조회 성공": (r) => r.status === 200,
    "상세 조회 응답시간 < 1초": (r) => r.timings.duration < 1000,
  });

  if (!success) errorRate.add(1);
  return response;
}

function checkBalance(userId) {
  const response = http.get(`${BASE_URL}/api/v1/users/${userId}/balance`, {
    tags: { name: "user_balance" },
  });

  const success = check(response, {
    "잔액 조회 성공": (r) => r.status === 200,
    "잔액 조회 응답시간 < 1초": (r) => r.timings.duration < 1000,
    "잔액 데이터 유효": (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true && body.data.balance !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) errorRate.add(1);
  return response;
}

function createOrder(userId, productId) {
  const orderData = {
    userId: userId,
    orderItems: [
      {
        productId: productId,
        quantity: Math.floor(Math.random() * 3) + 1, // 1-3개
      }
    ]
  };

  const response = http.post(
    `${BASE_URL}/api/v1/orders`, 
    JSON.stringify(orderData),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "create_order" },
    }
  );

  const success = check(response, {
    "주문 생성 성공": (r) => r.status === 201,
    "주문 생성 응답시간 < 5초": (r) => r.timings.duration < 5000,
    "주문 ID 존재": (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true && body.data.orderId !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) {
    errorRate.add(1);
    return null;
  }

  try {
    const body = JSON.parse(response.body);
    return body.data.orderId;
  } catch (e) {
    return null;
  }
}

function createOrderWithCoupon(userId, productId, couponId) {
  const orderData = {
    userId: userId,
    couponId: couponId, // 쿠폰 적용
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
      tags: { name: "create_order_with_coupon" },
    }
  );

  const success = check(response, {
    "쿠폰 주문 생성 성공": (r) => r.status === 201,
    "쿠폰 주문 응답시간 < 5초": (r) => r.timings.duration < 5000,
  });

  if (!success) {
    errorRate.add(1);
    return null;
  }

  try {
    const body = JSON.parse(response.body);
    return body.data.orderId;
  } catch (e) {
    return null;
  }
}

function getUserOrders(userId) {
  const response = http.get(`${BASE_URL}/api/v1/orders/users/${userId}`, {
    tags: { name: "user_orders" },
  });

  const success = check(response, {
    "사용자 주문 조회 성공": (r) => r.status === 200,
    "주문 조회 응답시간 < 2초": (r) => r.timings.duration < 2000,
    "주문 목록 데이터 유효": (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true && Array.isArray(body.data);
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) errorRate.add(1);
  return response;
}

function issueCoupon(userId, couponId) {
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
    }
  );

  const success = check(response, {
    "쿠폰 발급 요청 성공": (r) => r.status === 202, // ACCEPTED
    "쿠폰 발급 응답시간 < 3초": (r) => r.timings.duration < 3000,
    "요청 ID 존재": (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.success === true && body.data.requestId !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) errorRate.add(1);
  
  // 쿠폰 발급 상태 확인 (50% 확률)
  if (success && Math.random() < 0.5) {
    try {
      const body = JSON.parse(response.body);
      const requestId = body.data.requestId;
      
      // 1-3초 후 상태 확인 (실제 처리 시간 고려)
      const checkDelay = 1000 + Math.random() * 2000;
      sleep(checkDelay / 1000);
      
      checkCouponStatus(requestId);
    } catch (e) {
      console.log("쿠폰 상태 확인 실패:", e.message);
    }
  }

  return response;
}

function checkCouponStatus(requestId) {
  const response = http.get(`${BASE_URL}/api/coupons/async/status/${requestId}`, {
    tags: { name: "coupon_status" },
  });

  const success = check(response, {
    "쿠폰 상태 조회 성공": (r) => r.status === 200,
    "상태 조회 응답시간 < 1초": (r) => r.timings.duration < 1000,
  });

  if (!success) errorRate.add(1);
  return response;
}

// 테스트 종료 후 정리
export function teardown(data) {
  console.log("📊 부하 테스트 완료");
  console.log("📈 성공한 전체 트랜잭션:", successfulTransactions.count);
  
  // 간단한 결과 요약
  console.log("=====================================");
  console.log("🎯 테스트 요약:");
  console.log(`- 테스트 사용자 수: ${data.userIds.length}`);
  console.log(`- 테스트 상품 수: ${data.productIds.length}`);
  console.log(`- 쿠폰 종류: ${data.couponIds.length}`);
  console.log("=====================================");
}

// =============================================================================
// 유틸리티 함수
// =============================================================================

// 랜덤 지연 (인간의 자연스러운 행동 패턴)
function humanDelay() {
  const delay = 0.5 + Math.random() * 2; // 0.5-2.5초
  sleep(delay);
}

// 에러 로깅 (디버깅용)
function logError(api, response) {
  console.error(`❌ ${api} 실패: Status=${response.status}, Body=${response.body.substring(0, 200)}`);
}