package kr.hhplus.be.server.common.swagger;

import java.lang.annotation.*;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import kr.hhplus.be.server.common.response.CommonResponse;

/**
 * 수정된 공통 API 응답 어노테이션
 * 
 * 모든 API에서 발생할 수 있는 기본적인 에러 응답(400, 404, 500)을 정의하여
 * 개별 컨트롤러에서 반복적인 선언을 방지한다.
 * 
 * 🔧 수정사항:
 * - success: false로 수정
 * - 구체적인 에러 메시지 제공
 * - 현재 날짜로 timestamp 업데이트
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "잘못된 요청", value = """
                {
                  "success": false,
                  "error": {
                    "code": "INVALID_PARAMETER",
                    "message": "잘못된 요청 파라미터입니다."
                  },
                  "timestamp": "2025-07-24T15:30:00"
                }
                """))),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "리소스 없음", value = """
                {
                  "success": false,
                  "error": {
                    "code": "NOT_FOUND",
                    "message": "요청한 리소스를 찾을 수 없습니다."
                  },
                  "timestamp": "2025-07-24T15:30:00"
                }
                """))),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = @ExampleObject(name = "서버 오류", value = """
                {
                  "success": false,
                  "error": {
                    "code": "INTERNAL_ERROR",
                    "message": "서버 내부 오류가 발생했습니다."
                  },
                  "timestamp": "2025-07-24T15:30:00"
                }
                """)))
})
public @interface CommonApiResponses {
}