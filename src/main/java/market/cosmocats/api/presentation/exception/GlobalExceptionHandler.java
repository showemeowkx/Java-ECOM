package market.cosmocats.api.presentation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex, HttpServletRequest request) {
        Map<String, Object> errorBody = new HashMap<>();
        
        errorBody.put("type", "https://cosmo-cats.market/errors/internal");
        errorBody.put("title", "Something went wrong");
        errorBody.put("status", 500); 
        errorBody.put("detail", ex.getMessage());
        errorBody.put("instance", request.getRequestURI());

        return new ResponseEntity<>(errorBody, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
