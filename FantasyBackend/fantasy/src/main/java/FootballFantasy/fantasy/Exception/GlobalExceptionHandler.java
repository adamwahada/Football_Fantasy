package FootballFantasy.fantasy.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle UserAlreadyExistsException
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(
            UserAlreadyExistsException ex, WebRequest request) {

        Map<String, Object> errorResponse = createBaseErrorResponse(
                "USER_ALREADY_EXISTS",
                ex.getMessage(),
                request
        );

        System.out.println("❌ [EXCEPTION_HANDLER] User already exists: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle InsufficientBalanceException - This is key for your problem
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(
            InsufficientBalanceException ex, WebRequest request) {

        Map<String, Object> errorResponse = createBaseErrorResponse(
                "INSUFFICIENT_BALANCE",
                "You don't have enough balance to join this session",
                request
        );

        // Add specific balance details
        errorResponse.put("details", Map.of(
                "required", ex.getRequiredAmount(),
                "current", ex.getCurrentBalance(),
                "userId", ex.getUserId(),
                "shortage", calculateShortage(ex.getRequiredAmount(), ex.getCurrentBalance())
        ));

        // Add user-friendly suggestions
        errorResponse.put("suggestions", Map.of(
                "action", "Please add funds to your account",
                "minimumRequired", ex.getRequiredAmount()
        ));

        System.out.println("❌ [EXCEPTION_HANDLER] Insufficient balance for user " +
                ex.getUserId() + ": required=" + ex.getRequiredAmount() +
                ", current=" + ex.getCurrentBalance());

        // Return 422 (Unprocessable Entity) instead of 400 for business rule violations
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Handle BusinessLogicException
     */
    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessLogicException(
            BusinessLogicException ex, WebRequest request) {

        Map<String, Object> errorResponse = createBaseErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                request
        );

        System.out.println("❌ [EXCEPTION_HANDLER] Business logic error: " + ex.getMessage() +
                " (Code: " + ex.getErrorCode() + ")");

        // Return 422 for business rule violations instead of 400
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Handle validation errors (from @Valid annotations)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> validationErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage(),
                        (existing, replacement) -> existing // Keep first error if multiple for same field
                ));

        Map<String, Object> errorResponse = createBaseErrorResponse(
                "VALIDATION_ERROR",
                "Invalid input data",
                request
        );

        errorResponse.put("validationErrors", validationErrors);

        System.out.println("❌ [EXCEPTION_HANDLER] Validation errors: " + validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        Map<String, Object> errorResponse = createBaseErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                request
        );

        System.err.println("❌ [EXCEPTION_HANDLER] Invalid argument: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle RuntimeException - but skip specific exceptions already handled
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        // Skip if it's already handled by more specific handlers
        if (ex instanceof BusinessLogicException ||
                ex instanceof InsufficientBalanceException ||
                ex instanceof UserAlreadyExistsException) {
            throw ex; // Let the specific handlers deal with it
        }

        Map<String, Object> errorResponse = createBaseErrorResponse(
                "RUNTIME_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected runtime error occurred",
                request
        );

        System.err.println("❌ [EXCEPTION_HANDLER] Runtime error: " + ex.getMessage());
        ex.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Catch-all for unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {

        Map<String, Object> errorResponse = createBaseErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                request
        );

        // Log the full exception for debugging but don't expose details to client
        System.err.println("❌ [EXCEPTION_HANDLER] Unexpected error: " + ex.getMessage());
        System.err.println("❌ [EXCEPTION_HANDLER] Exception type: " + ex.getClass().getSimpleName());
        ex.printStackTrace();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Create a consistent base error response structure
     */
    private Map<String, Object> createBaseErrorResponse(String errorCode, String message, WebRequest request) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        return errorResponse;
    }

    /**
     * Calculate shortage amount for insufficient balance
     */
    private String calculateShortage(String required, String current) {
        try {
            double requiredAmount = Double.parseDouble(required);
            double currentAmount = Double.parseDouble(current);
            double shortage = requiredAmount - currentAmount;
            return String.format("%.2f", Math.max(0, shortage));
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }
}