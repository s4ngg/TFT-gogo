package com.tftgogo.global.exception;

import com.tftgogo.global.response.ApiResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

    // ── 비즈니스 예외 ─────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        logger.warn(
                "BusinessException - Code: {}, Status: {}",
                e.getErrorCode().name(),
                e.getErrorCode().getStatus()
        );
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(e.getErrorCode().getStatus());
        if (e.getRetryAfterSeconds() > 0) {
            builder.header("Retry-After", String.valueOf(e.getRetryAfterSeconds()));
        }
        return builder.body(ApiResponse.fail(
                e.getErrorCode().getMessage(),
                e.getErrorCode().getStatus()
        ));
    }

    // ── @Valid 검증 실패 ───────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        logger.warn("ValidationException - Field: {}, Message: {}",
                e.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(FieldError::getField)
                        .orElse("unknown"),
                message
        );
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.fail(
                        message,
                        ErrorCode.INVALID_INPUT.getStatus()
                ));
    }

    // ── enum / 타입 변환 실패 (@RequestParam, @PathVariable) ──
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = String.format("'%s' 파라미터에 유효하지 않은 값입니다: %s", e.getName(), e.getValue());
        logger.warn("TypeMismatchException - param: {}, value: {}", e.getName(), e.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(message, HttpStatus.BAD_REQUEST));
    }

    // ── 그 외 예상치 못한 예외 ─────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        logger.error("Unhandled exception: {}", e.getClass().getSimpleName(), e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.fail(
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getStatus()
                ));
    }
}