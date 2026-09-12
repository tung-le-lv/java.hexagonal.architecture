package com.acme.orders.adapter.inbound.rest;

import com.acme.orders.application.exception.ApplicationException;
import com.acme.orders.application.exception.ConcurrentModificationException;
import com.acme.orders.application.exception.InvalidCommandException;
import com.acme.orders.application.exception.OrderNotFoundException;
import com.acme.orders.domain.exception.DomainException;
import com.acme.orders.domain.exception.EmptyOrderException;
import com.acme.orders.domain.exception.InvalidOrderStateException;
import com.acme.orders.domain.exception.OrderLineNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates core failures into HTTP status codes, in the one place that is allowed to know about HTTP.
 *
 * <p>This is the other half of the inbound adapter's job. The domain throws
 * {@link InvalidOrderStateException} because a rule was broken; deciding that this means {@code 409
 * Conflict} is a transport decision and belongs here. That separation is what lets the same core sit
 * behind a message consumer, where the same exception would mean "route to the dead-letter queue".
 *
 * <p>Mapping is driven by the stable {@code code} each exception carries, so new exception types get
 * a sensible default rather than a 500.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);
    private static final String CODE = "code";

    /** The order (or one of its lines) does not exist -> 404. */
    @ExceptionHandler({OrderNotFoundException.class, OrderLineNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception, codeOf(exception));
    }

    /** The request was understood but the order's state forbids it -> 409. */
    @ExceptionHandler({InvalidOrderStateException.class, EmptyOrderException.class})
    public ProblemDetail handleConflict(DomainException exception) {
        return problem(HttpStatus.CONFLICT, "Order state conflict", exception, exception.code());
    }

    /** Someone else changed the order first -> 409, and retrying is the expected response. */
    @ExceptionHandler(ConcurrentModificationException.class)
    public ProblemDetail handleConcurrentModification(ConcurrentModificationException exception) {
        return problem(HttpStatus.CONFLICT, "Concurrent modification", exception, exception.code());
    }

    /** Any other broken business rule -> 422: well-formed, but not acceptable. */
    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainRule(DomainException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation", exception, exception.code());
    }

    /** A structurally unusable command -> 400. */
    @ExceptionHandler(InvalidCommandException.class)
    public ProblemDetail handleInvalidCommand(InvalidCommandException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception, exception.code());
    }

    /** Anything else the application reports explicitly -> 422. */
    @ExceptionHandler(ApplicationException.class)
    public ProblemDetail handleApplication(ApplicationException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Request could not be processed", exception, exception.code());
    }

    /** Bean Validation failures -> 400, with the offending fields listed. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        exception.getBindingResult().getGlobalErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("the request body is not valid");
        problem.setProperty(CODE, "request.validation_failed");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /**
     * A path variable or query parameter that cannot be converted -> 400.
     *
     * <p>Without this the container's type conversion failure would reach the catch-all below and be
     * reported as a 500, blaming the server for the client's malformed URL.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail("'" + exception.getName() + "' is not a valid value");
        problem.setProperty(CODE, "request.malformed_parameter");
        return problem;
    }

    /** A required query parameter was omitted -> 400. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail("required parameter '" + exception.getParameterName() + "' is missing");
        problem.setProperty(CODE, "request.missing_parameter");
        return problem;
    }

    /** A body that is not parseable JSON, or whose types do not fit -> 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request");
        problem.setDetail("the request body could not be read as JSON");
        problem.setProperty(CODE, "request.malformed_body");
        return problem;
    }

    /**
     * Last resort. The message is withheld deliberately — an unexpected failure's message may expose
     * internals — while the stack trace goes to the log where operators can see it.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        log.error("unhandled exception while serving request", exception);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal server error");
        problem.setDetail("the request could not be completed");
        problem.setProperty(CODE, "internal_error");
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, Throwable exception, String code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(exception.getMessage());
        problem.setProperty(CODE, code);
        return problem;
    }

    private static String codeOf(RuntimeException exception) {
        if (exception instanceof DomainException domainException) {
            return domainException.code();
        }
        if (exception instanceof ApplicationException applicationException) {
            return applicationException.code();
        }
        return "error";
    }
}
