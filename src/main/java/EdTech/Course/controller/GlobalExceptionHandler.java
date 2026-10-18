package EdTech.Course.controller;

import EdTech.Course.dto.ResponseMessage;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseMessage handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return new ResponseMessage("Validation failed: " + errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseMessage handleIllegalArgument(IllegalArgumentException ex) {
        return new ResponseMessage(ex.getMessage() != null ? ex.getMessage() : "Invalid argument");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseMessage handleAccessDenied(AccessDeniedException ex) {
        return new ResponseMessage("Access denied: " + ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseMessage handleNotFound(NoSuchElementException ex) {
        return new ResponseMessage(ex.getMessage() != null ? ex.getMessage() : "Resource not found");
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseMessage handleRuntimeException(RuntimeException ex) {
        return new ResponseMessage(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseMessage handleGenericException(Exception ex) {
        return new ResponseMessage("Internal server error");
    }
}

