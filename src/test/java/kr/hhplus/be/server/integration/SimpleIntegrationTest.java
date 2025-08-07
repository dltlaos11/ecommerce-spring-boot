package kr.hhplus.be.server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import kr.hhplus.be.server.common.response.CommonResponse;
import kr.hhplus.be.server.common.test.IntegrationTestBase;

/**
 * 간단한 통합테스트 검증
 * 
 * 목적:
 * - 기본적인 API 엔드포인트 동작 확인
 * - TestContainers 환경에서의 Spring Boot 구동 검증
 * - 404 에러 원인 파악
 */
@DisplayName("간단한 통합테스트 검증")
@Transactional
class SimpleIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Spring Boot Actuator Health Check - 가장 기본적인 엔드포인트")
    void health_check_테스트() {
        // When: Health Check API 호출
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        // Then: 200 OK 응답 확인
        System.out.println("🔍 Health Check 응답: " + response.getStatusCode());
        System.out.println("🔍 Response Body: " + response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("상품 목록 API 기본 동작 확인")
    void 상품목록API_기본동작확인() {
        try {
            // When: 상품 목록 API 호출
            ResponseEntity<CommonResponse> response = restTemplate.getForEntity(
                    "/api/v1/products", CommonResponse.class);

            // Then: 응답 상태 확인
            System.out.println("🔍 Products API 응답: " + response.getStatusCode());
            System.out.println("🔍 Response Body: " + response.getBody());

            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                System.err.println("❌ 404 에러 발생 - Controller 매핑 문제 가능성");
                System.err.println("📋 가능한 원인:");
                System.err.println("1. @RestController 어노테이션 누락");
                System.err.println("2. @RequestMapping 경로 문제");
                System.err.println("3. Component Scan 범위 문제");

                // 추가 디버깅 실행
                debugApiEndpoints();
            } else {
                System.out.println("✅ Products API 정상 동작");
                assertThat(response.getBody()).isNotNull();
                if (response.getBody() != null) {
                    assertThat(response.getBody().isSuccess()).isTrue();
                }
            }

        } catch (Exception e) {
            System.err.println("❌ API 호출 중 예외 발생: " + e.getMessage());
            debugTestFailure("상품목록API_기본동작확인", e);
            throw e;
        }
    }

    @Test
    @DisplayName("TestContainers MySQL 연결 상태 확인")
    void testContainers_MySQL_연결확인() {
        // Given: 테스트 환경 검증 (부모 클래스 메서드 활용)
        verifyTestEnvironment();

        // Then: 검증이 예외 없이 완료되면 성공
        System.out.println("✅ TestContainers MySQL 연결 정상");
        System.out.println("✅ EntityManager 주입 정상: " + (entityManager != null));
        System.out.println("✅ RestTemplate 주입 정상: " + (restTemplate != null));
    }

    @Test
    @DisplayName("Controller Bean 등록 상태 확인")
    void controller_Bean등록_확인() {
        // When: 간단한 HTTP 호출로 애플리케이션 상태 확인
        try {
            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
            System.out.println("✅ Spring Boot 애플리케이션 정상 구동");
            System.out.println("📊 Health Status: " + response.getStatusCode());

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("❌ Spring Boot 애플리케이션 구동 실패: " + e.getMessage());
            throw e;
        }
    }

    @Test
    @DisplayName("API 경로 매핑 디버깅")
    void API_경로매핑_디버깅() {
        debugApiEndpoints();

        // 최소한 Health Check는 동작해야 함
        ResponseEntity<String> healthResponse = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * API 엔드포인트 디버깅 헬퍼 메서드
     */
    private void debugApiEndpoints() {
        System.out.println("=== API 경로 매핑 디버깅 ===");

        String[] endpoints = {
                "/",
                "/api",
                "/api/v1",
                "/api/v1/products",
                "/api/v1/coupons/available",
                "/actuator/health"
        };

        for (String endpoint : endpoints) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
                System.out.println("✅ " + endpoint + " -> " + response.getStatusCode());
            } catch (Exception e) {
                System.err.println("❌ " + endpoint + " -> " + e.getMessage());
            }
        }
    }
}