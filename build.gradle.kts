plugins {
	java
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
}

fun getGitHash(): String {
	return providers.exec {
		commandLine("git", "rev-parse", "--short", "HEAD")
	}.standardOutput.asText.get().trim()
}

group = "kr.hhplus.be"
version = getGitHash()

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
		mavenBom("org.testcontainers:testcontainers-bom:1.19.0") // TestContainers BOM 추가
	}
}

dependencies {
    // Spring Boot 기본
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	
	// Swagger/OpenAPI
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
	
	// Validation
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

    // Database
	runtimeOnly("com.mysql:mysql-connector-j")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
	
	// TestContainers 의존성 추가
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
	
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	
	// Test Lombok
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("user.timezone", "UTC")
	
	// TestContainers 설정
	systemProperty("testcontainers.reuse.enable", "false") // 재사용 비활성화로 안정성 확보
	systemProperty("testcontainers.ryuk.disabled", "false") // Ryuk 컨테이너 정리 활성화
	
	// 테스트 실행 설정
	maxParallelForks = 1 // 순차 실행으로 안정성 확보
	forkEvery = 1 // 각 테스트 클래스마다 새로운 JVM
	
	// 메모리 설정 증가
	minHeapSize = "1g"
	maxHeapSize = "3g"
	
	// JVM 옵션 추가
	jvmArgs(
		"-XX:+UseG1GC",
		"-XX:MaxGCPauseMillis=200",
		"-Djava.awt.headless=true",
		"-Dspring.jmx.enabled=false"
	)
	
	// 테스트 타임아웃 설정 - 통합테스트에 맞게 조정
	systemProperty("junit.jupiter.execution.timeout.default", "15m")
	systemProperty("junit.jupiter.execution.timeout.testable.method.default", "8m")
	
	// 로깅 레벨 조정
	systemProperty("logging.level.org.testcontainers", "INFO")
	systemProperty("logging.level.com.github.dockerjava", "WARN")
	
	// 테스트 실패 시 상세 정보 출력
	testLogging {
		events("passed", "skipped", "failed")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showExceptions = true
		showCauses = true
		showStackTraces = true
	}
}