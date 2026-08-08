package in.ankush.cloudshareapi.expections;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- Auth / JWT related (new) ----------

    // Bad email/password on /auth/login
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // Unknown email on /auth/login
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    // Any other authentication failure (e.g. invalid/expired JWT reaching a controller)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication failed: " + ex.getMessage());
    }

    // Authenticated, but role/permission doesn't allow this action (e.g. USER hitting an ADMIN API)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    // @Valid failures on RegisterRequest / LoginRequest bodies -> 400 with field-level messages
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", Instant.now().toString());
        data.put("status", HttpStatus.BAD_REQUEST.value());
        data.put("message", "Validation failed");
        data.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(data);
    }

    // Bad request payload (e.g. missing profile photo file)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ---------- Existing handlers (unchanged behaviour) ----------

    // Duplicate email on /auth/register (unique index on User.email), or any other duplicate-key write
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<?> handleDuplicateEmailException(DuplicateKeyException ex) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", HttpStatus.CONFLICT.value());
        data.put("message", "Duplicate entry: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(data);
    }

    // ✅ RuntimeException handle karo - proper error messages milenge
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> data = new HashMap<>();

        String message = ex.getMessage();

        if (message != null && message.contains("Access denied")) {
            data.put("status", HttpStatus.FORBIDDEN.value());
            data.put("message", message);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(data);
        }

        if (message != null && message.contains("not found")) {
            data.put("status", HttpStatus.NOT_FOUND.value());
            data.put("message", message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(data);
        }

        if (message != null && message.contains("Not enough credits")) {
            data.put("status", HttpStatus.PAYMENT_REQUIRED.value());
            data.put("message", message);
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(data);
        }

        data.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        data.put("message", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(data);
    }

    private ResponseEntity<?> buildResponse(HttpStatus status, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", Instant.now().toString());
        data.put("status", status.value());
        data.put("message", message);
        return ResponseEntity.status(status).body(data);
    }
}
