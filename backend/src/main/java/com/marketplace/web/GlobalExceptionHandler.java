package com.marketplace.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ProblemDetail handleDatabase(org.springframework.dao.DataAccessException ex) {
        return databaseFailure(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Database operation failed");
    }

    private ProblemDetail databaseFailure(Throwable ex, HttpStatus fallback, String detail) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof java.sql.SQLException sql) {
                if ("42501".equals(sql.getSQLState())) {
                    return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                            "Database account lacks the permissions required for this operation");
                }
                if (sql.getSQLState() != null && sql.getSQLState().startsWith("23")) {
                    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                            "Operation conflicts with existing records or database constraints");
                }
            }
            cause = cause.getCause();
        }
        return ProblemDetail.forStatusAndDetail(fallback, detail);
    }

    @ExceptionHandler(org.springframework.jdbc.datasource.init.ScriptException.class)
    public ProblemDetail handleMigration(org.springframework.jdbc.datasource.init.ScriptException ex) {
        return databaseFailure(ex, HttpStatus.CONFLICT,
                "Tenant migration failed; assignment was rolled back. Check schema compatibility and permissions");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setDetail("Validation failed");
        return detail;
    }
}
