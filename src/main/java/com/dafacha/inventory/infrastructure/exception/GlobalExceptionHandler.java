package com.dafacha.inventory.infrastructure.exception;

import com.dafacha.inventory.application.dto.response.ErrorResponse;
import com.dafacha.inventory.domain.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleApiException(ApiException ex, ServerWebExchange exchange) {
        log.warn("[{}] {} - {}", ex.getCode(), exchange.getRequest().getPath().value(), ex.getMessage());
        return Mono.just(buildResponse(ex.getStatus(), ex.getCode(), ex.getMessage(), exchange));
    }

    /**
     * Framework-raised HTTP errors (unmapped route -> 404, wrong method -> 405, bad body -> 400, ...).
     * Without this they would fall into the catch-all below and be reported as a misleading 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleResponseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.warn("[{}] {} - {}", status.value(), exchange.getRequest().getPath().value(), ex.getReason());
        // The framework's own reasons are technical English ("Failed to read HTTP message"); the user gets Spanish.
        String message = switch (status) {
            case BAD_REQUEST -> "La solicitud no es válida";
            case NOT_FOUND -> "No se encontró lo que buscas";
            case METHOD_NOT_ALLOWED -> "Método no permitido";
            case UNSUPPORTED_MEDIA_TYPE -> "Formato de datos no admitido";
            default -> "No se pudo procesar la solicitud";
        };
        return Mono.just(buildResponse(status, status.name(), message, exchange));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getFieldErrors().stream()
                // The messages already say which field they are about ("El nombre es obligatorio").
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed on {}: {}", exchange.getRequest().getPath().value(), message);
        return Mono.just(buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, exchange));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleMethodValidation(
            HandlerMethodValidationException ex,
            ServerWebExchange exchange
    ) {
        String message = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Method validation failed on {}: {}", exchange.getRequest().getPath().value(), message);
        return Mono.just(buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, exchange));
    }

    @ExceptionHandler(AuthenticationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAuthentication(AuthenticationException ex, ServerWebExchange exchange) {
        log.warn("Authentication failed on {}: {}", exchange.getRequest().getPath().value(), ex.getMessage());
        return Mono.just(buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Debes iniciar sesión", exchange));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAccessDenied(AccessDeniedException ex, ServerWebExchange exchange) {
        log.warn("Access denied on {}: {}", exchange.getRequest().getPath().value(), ex.getMessage());
        return Mono.just(buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "Acceso denegado", exchange));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDataIntegrity(DataIntegrityViolationException ex, ServerWebExchange exchange) {
        log.warn("Data integrity violation on {}: {}", exchange.getRequest().getPath().value(), ex.getMessage());
        return Mono.just(buildResponse(
                HttpStatus.CONFLICT,
                "CONFLICT",
                "La operación entra en conflicto con datos existentes",
                exchange
        ));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled exception on {}", exchange.getRequest().getPath().value(), ex);
        return Mono.just(buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Ocurrió un error inesperado",
                exchange
        ));
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            ServerWebExchange exchange
    ) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(exchange.getRequest().getPath().value())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
