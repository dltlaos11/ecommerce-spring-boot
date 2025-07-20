package kr.hhplus.be.server.common.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 API 응답")
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값은 JSON에 포함하지 않음
public class CommonResponse<T> {

    @Schema(description = "성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 데이터")
    private final T data;

    @Schema(description = "에러 정보")
    private final ErrorResponse error;

    @Schema(description = "응답 시간", example = "2025-07-17T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    private CommonResponse(boolean success, T data, ErrorResponse error, LocalDateTime timestamp) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = timestamp;
    }

    /**
     * 성공 응답 생성
     * 
     * @param data 응답 데이터 (error는 자동으로 null 처리)
     */
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(true, data, null, LocalDateTime.now());
    }

    /**
     * 실패 응답 생성
     * 
     * @param code    에러 코드
     * @param message 에러 메시지 (data는 자동으로 null 처리)
     */
    public static <T> CommonResponse<T> error(String code, String message) {
        ErrorResponse errorResponse = new ErrorResponse(code, message);
        return new CommonResponse<>(false, null, errorResponse, LocalDateTime.now());
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorResponse getError() {
        return error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}