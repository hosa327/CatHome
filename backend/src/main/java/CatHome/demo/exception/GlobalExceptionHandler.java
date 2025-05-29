package CatHome.demo.exception;


import CatHome.demo.dto.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("msg", e.getMessage()));
    }

    @ExceptionHandler(EmailException.class)
    public ResponseEntity<?> handleEmailAlreadyExists(EmailException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("msg", ex.getMessage())); // 409 Conflict
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<?> handleUserNotFound(UserException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("msg", ex.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleIOException(IOException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("msg", "Fail to read the file：" + ex.getMessage())); // 500 Internal Server Error
    }

    @ExceptionHandler(ConnectionException.class)
    public ResponseEntity<?> handleConnectionException(UserException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("msg", ex.getMessage()));
    }

    @ExceptionHandler(TopicException.class)
    public ResponseEntity<?> handleTopicException(TopicException ex) {
        ApiResponse response = new ApiResponse<Void>(1, "Error: " + ex.getMessage(), null);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ApiResponse> handleJsonException(JsonProcessingException ex){
        ApiResponse response = new ApiResponse<Void>(1, "Error: " + ex.getMessage(), null);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

}