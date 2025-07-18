package kr.hhplus.be.server.common.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

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