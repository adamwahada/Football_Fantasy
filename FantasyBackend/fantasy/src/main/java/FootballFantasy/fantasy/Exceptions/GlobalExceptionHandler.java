package FootballFantasy.fantasy.Exceptions;

import FootballFantasy.fantasy.Exceptions.PaiementExceptions.*;
import FootballFantasy.fantasy.Exceptions.PrivateSessionsExceptions.PrivateSessionFullException;
import FootballFantasy.fantasy.Exceptions.PrivateSessionsExceptions.PrivateSessionGameweekMismatchException;
import FootballFantasy.fantasy.Exceptions.PrivateSessionsExceptions.PrivateSessionNotFoundException;
import FootballFantasy.fantasy.Exceptions.UsersExceptions.UserAlreadyExistsException;
import FootballFantasy.fantasy.Exceptions.UsersExceptions.UserBannedException;
import FootballFantasy.fantasy.Exceptions.UsersExceptions.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.nio.file.AccessDeniedException;
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

        // Add specific handling for deadline passed
        if ("DEADLINE_PASSED".equals(ex.getErrorCode())) {
            errorResponse.put("message", "La date limite d'inscription est dépassée");
            errorResponse.put("suggestions", Map.of(
                    "message", "Cette session n'accepte plus de nouveaux participants",
                    "alternativeAction", "Consultez d'autres sessions disponibles"
            ));
        }

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

    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<Map<String, Object>> handleUserBannedException(
            UserBannedException ex, WebRequest request) {

        Map<String, Object> errorResponse = createBaseErrorResponse(
                "USER_BANNED",
                ex.getMessage(),
                request
        );

        System.out.println("❌ [EXCEPTION_HANDLER] Banned user attempted action: " + ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    /**
     * Handle PrivateSessionNotFoundException
     */
    @ExceptionHandler(PrivateSessionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePrivateSessionNotFound(
            PrivateSessionNotFoundException ex, WebRequest request) {

        Map<String, Object> errorResponse = createBaseErrorResponse(
                ex.getErrorCode(),
                "Aucune session privée trouvée avec cette clé d'accès",
                request
        );

        // Add specific details for private session not found
        errorResponse.put("details", Map.of(
                "accessKey", ex.getAccessKey(),
                "action", "Vérifiez votre clé d'accès et réessayez"
        ));

        errorResponse.put("suggestions", Map.of(
                "message", "Assurez-vous d'avoir la bonne clé d'accès du créateur de la session",
                "contact", "Contactez la personne qui vous a partagé cette clé d'accès"
        ));

        System.out.println("❌ [EXCEPTION_HANDLER] Private session not found with access key: " + ex.getAccessKey());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle PrivateSessionGameweekMismatchException
     */
    @ExceptionHandler(PrivateSessionGameweekMismatchException.class)
    public ResponseEntity<Map<String, Object>> handlePrivateSessionGameweekMismatch(
            PrivateSessionGameweekMismatchException ex, WebRequest request) {

        Map<String, Object> errorResponse = createBaseErrorResponse(
                ex.getErrorCode(),
                "Cette clé d'accès est pour une gameweek différente",
                request
        );

        // Add specific details for gameweek mismatch
        errorResponse.put("details", Map.of(
                "accessKey", ex.getAccessKey(),
                "requestedGameweek", ex.getRequestedGameweekId(),
                "actualGameweek", ex.getActualGameweekId(),
                "action", "Cette clé d'accès est pour la gameweek " + ex.getActualGameweekId()
        ));

        errorResponse.put("suggestions", Map.of(
                "message", "Utilisez cette clé d'accès pour la gameweek " + ex.getActualGameweekId(),
                "alternativeAction", "Demandez la bonne clé d'accès pour la gameweek " + ex.getRequestedGameweekId()
        ));

        System.out.println("❌ [EXCEPTION_HANDLER] Private session gameweek mismatch: " +
                "requested=" + ex.getRequestedGameweekId() +
                ", actual=" + ex.getActualGameweekId() +
                ", accessKey=" + ex.getAccessKey());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle PrivateSessionFullException
     */
    @ExceptionHandler(PrivateSessionFullException.class)
    public ResponseEntity<Map<String, Object>> handlePrivateSessionFull(
            PrivateSessionFullException ex, WebRequest request) {

        Map<String, Object> errorResponse = createBaseErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                request
        );

        // Add specific details for full session
        errorResponse.put("details", Map.of(
                "accessKey", ex.getAccessKey(),
                "sessionId", ex.getSessionId(),
                "currentParticipants", ex.getCurrentParticipants(),
                "maxParticipants", ex.getMaxParticipants(),
                "action", "This private session is full"
        ));

        errorResponse.put("suggestions", Map.of(
                "message", "Contact the session creator to see if they can create a new session",
                "alternativeAction", "Try joining a public session instead"
        ));

        System.out.println("❌ [EXCEPTION_HANDLER] Private session full: " +
                "sessionId=" + ex.getSessionId() +
                ", participants=" + ex.getCurrentParticipants() + "/" + ex.getMaxParticipants() +
                ", accessKey=" + ex.getAccessKey());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(DepositNotFoundException.class)
    public ResponseEntity<String> handleDepositNotFound(DepositNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(DepositAlreadyProcessedException.class)
    public ResponseEntity<Map<String, String>> handleDepositAlreadyProcessed(DepositAlreadyProcessedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "DEPOSIT_ALREADY_PROCESSED",
                        "depositId", String.valueOf(ex.getDepositId()),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(WithdrawNotAvailableException.class)
    public ResponseEntity<Map<String, String>> handleWithdrawNotAvailable(WithdrawNotAvailableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "WITHDRAW_NOT_AVAILABLE",
                        "message", ex.getMessage()
                ));
    }


    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "USER_NOT_FOUND",
                        "keycloakId", ex.getKeycloakId(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(WithdrawLimitExceededException.class)
    public ResponseEntity<Object> handleWithdrawLimitExceeded(WithdrawLimitExceededException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("error", "WITHDRAW_LIMIT_EXCEEDED");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WithdrawReservationExpiredException.class)
    public ResponseEntity<Object> handleWithdrawReservationExpired(WithdrawReservationExpiredException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("error", "WITHDRAW_RESERVATION_EXPIRED");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WithdrawNotReservedByUserException.class)
    public ResponseEntity<Object> handleWithdrawNotReservedByUser(WithdrawNotReservedByUserException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("error", "WITHDRAW_NOT_RESERVED_BY_USER");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WithdrawNotFoundException.class)
    public ResponseEntity<Object> handleWithdrawNotFound(WithdrawNotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("error", "WITHDRAW_NOT_FOUND");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedWithdrawCancellationException.class)
    public ResponseEntity<Object> handleUnauthorizedWithdrawCancellation(UnauthorizedWithdrawCancellationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("error", "UNAUTHORIZED_WITHDRAW_CANCELLATION");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidWithdrawStatusException.class)
    public ResponseEntity<Object> handleInvalidWithdrawStatus(InvalidWithdrawStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("error", "INVALID_WITHDRAW_STATUS");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WithdrawReservedException.class)
    public ResponseEntity<Object> handleWithdrawReserved(WithdrawReservedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("error", "WITHDRAW_RESERVED");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<Object> handleAccessDenied(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("success", false);
        body.put("error", "ACCESS_DENIED");
        body.put("message", "You do not have permission to access this resource");

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }



}