package kr.hhplus.be.server.common.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Swagger/OpenAPI 3.0 설정
 * 
 * Mock API 단계에서 프론트엔드와의 협업 및 API 테스트를 위해
 * 상세한 API 명세를 자동 생성한다.
 * 
 * 실제 구현 시에는 RestDocs로 대체 검토 필요.
 */
@Configuration
public class SwaggerConfig {

        /**
         * E-커머스 API 문서화를 위한 OpenAPI 설정
         * 
         * @return OpenAPI 설정 객체
         */
        @Bean
        public OpenAPI openAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("E-커머스 API")
                                                .description("E-커머스 상품 주문 서비스 API 명세서")
                                                .version("v1.0.0"))
                                .addServersItem(new Server()
                                                .url("http://localhost:8080")
                                                .description("로컬 개발 서버"));
        }
}