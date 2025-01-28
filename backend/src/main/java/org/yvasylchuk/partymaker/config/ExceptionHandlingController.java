package org.yvasylchuk.partymaker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.yvasylchuk.partymaker.exception.PartymakerException;

@Slf4j
@RestControllerAdvice
public class ExceptionHandlingController {
    @ExceptionHandler(PartymakerException.class)
    public ResponseEntity<ErrorResponseBody> handleInternalException(PartymakerException e) {
        log.info("Exception is intercepted", e);
        HttpStatusCode code = switch (e.getKind()) {
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case GENERIC_CLIENT, INVALID_CONFIGURATION -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(code)
                             .body(new ErrorResponseBody(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleException(Exception e) {
        log.error("Some unhandled error", e);
    }

    public record ErrorResponseBody(String message) {
    }
}
