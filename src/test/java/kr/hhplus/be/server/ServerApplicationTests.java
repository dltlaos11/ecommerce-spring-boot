package kr.hhplus.be.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 기본 애플리케이션 컨텍스트 로드 테스트
 * 
 * 수정사항:
 * - TestContainers 설정 추가
 * - @Import 제거 (자동 감지)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ServerApplicationTests {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
			.withDatabaseName("testdb")
			.withUsername("test")
			.withPassword("test");

	@Test
	void contextLoads() {
		// Spring Boot 애플리케이션이 정상적으로 시작되는지 테스트
	}
}