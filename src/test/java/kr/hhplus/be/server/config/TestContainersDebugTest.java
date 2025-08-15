package kr.hhplus.be.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * TestContainers 디버깅용 간단한 테스트
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
class TestContainersDebugTest {

    @Test
    void testContainers가_정상적으로_시작되는지_확인() {
        // 컨텍스트가 정상적으로 로드되면 성공
        System.out.println("TestContainers 설정이 정상적으로 로드되었습니다.");
    }
}