package com.enterprise.app.presentation.exception;

import com.enterprise.app.domain.exception.BusinessException;
import com.enterprise.app.domain.exception.DuplicateResourceException;
import com.enterprise.app.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    @Mock
    private WebRequest webRequest;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        // Set default locale
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        // Mock web request description
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/test");
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException")
    void shouldHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResourceNotFoundException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("User not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().getTimestamp()).isNotNull();

        verify(webRequest).getDescription(false);
    }

    @Test
    @DisplayName("Should handle DuplicateResourceException")
    void shouldHandleDuplicateResourceException() {
        // Given
        DuplicateResourceException exception = new DuplicateResourceException(
            "User with email test@example.com already exists");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleDuplicateResourceException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage())
            .isEqualTo("User with email test@example.com already exists");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");

        verify(webRequest).getDescription(false);
    }

    @Test
    @DisplayName("Should handle BusinessException")
    void shouldHandleBusinessException() {
        // Given
        BusinessException exception = new BusinessException("Invalid operation");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleBusinessException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid operation");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");

        verify(webRequest).getDescription(false);
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException")
    void shouldHandleMethodArgumentNotValidException() throws NoSuchMethodException {
        // Given
        FieldError fieldError1 = new FieldError("user", "email", "must be a valid email");
        FieldError fieldError2 = new FieldError("user", "firstName", "must not be blank");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // Create a real MethodParameter from a real method to avoid NullPointerException
        MethodParameter methodParameter = new MethodParameter(
            this.getClass().getDeclaredMethod("dummyMethod", String.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
            methodParameter, bindingResult);

        when(messageSource.getMessage(eq("common.validation.error"), isNull(), any(Locale.class)))
            .thenReturn("Validation failed");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleValidationExceptions(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().getValidationErrors()).isNotNull();
        assertThat(response.getBody().getValidationErrors()).hasSize(2);
        assertThat(response.getBody().getValidationErrors().get("email"))
            .isEqualTo("must be a valid email");
        assertThat(response.getBody().getValidationErrors().get("firstName"))
            .isEqualTo("must not be blank");

        verify(messageSource).getMessage(eq("common.validation.error"), isNull(), any(Locale.class));
        verify(webRequest).getDescription(false);
    }

    @Test
    @DisplayName("Should handle AuthenticationException")
    void shouldHandleAuthenticationException() {
        // Given
        AuthenticationException exception = new AuthenticationException("Bad credentials") {};

        when(messageSource.getMessage(eq("error.unauthorized"), isNull(), any(Locale.class)))
            .thenReturn("Unauthorized access");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleAuthenticationException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        assertThat(response.getBody().getMessage()).isEqualTo("Unauthorized access");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");

        verify(messageSource).getMessage(eq("error.unauthorized"), isNull(), any(Locale.class));
        verify(webRequest).getDescription(false);
    }

    @Test
    @DisplayName("Should handle AccessDeniedException")
    void shouldHandleAccessDeniedException() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        when(messageSource.getMessage(eq("error.forbidden"), isNull(), any(Locale.class)))
            .thenReturn("Forbidden access");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleAccessDeniedException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        assertThat(response.getBody().getMessage()).isEqualTo("Forbidden access");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");

        verify(messageSource).getMessage(eq("error.forbidden"), isNull(), any(Locale.class));
        verify(webRequest).getDescription(false);
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void shouldHandleGenericException() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        when(messageSource.getMessage(eq("error.internal"), isNull(), any(Locale.class)))
            .thenReturn("Internal server error");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleGlobalException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/test");

        verify(messageSource).getMessage(eq("error.internal"), isNull(), any(Locale.class));
        verify(webRequest).getDescription(false);
    }

    @Test
    @DisplayName("Should handle NullPointerException as generic exception")
    void shouldHandleNullPointerException() {
        // Given
        NullPointerException exception = new NullPointerException("Null value");

        when(messageSource.getMessage(eq("error.internal"), isNull(), any(Locale.class)))
            .thenReturn("Internal server error");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleGlobalException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);

        verify(messageSource).getMessage(eq("error.internal"), isNull(), any(Locale.class));
    }

    @Test
    @DisplayName("Should extract path from WebRequest correctly")
    void shouldExtractPathFromWebRequest() {
        // Given
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/users/123");
        ResourceNotFoundException exception = new ResourceNotFoundException("User not found");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResourceNotFoundException(exception, webRequest);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/users/123");

        verify(webRequest).getDescription(false);
    }

    @Test
    @DisplayName("Should handle validation errors with empty field list")
    void shouldHandleValidationWithEmptyFields() throws NoSuchMethodException {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of());

        // Create a real MethodParameter from a real method to avoid NullPointerException
        MethodParameter methodParameter = new MethodParameter(
            this.getClass().getDeclaredMethod("dummyMethod", String.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
            methodParameter, bindingResult);

        when(messageSource.getMessage(eq("common.validation.error"), isNull(), any(Locale.class)))
            .thenReturn("Validation failed");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleValidationExceptions(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getValidationErrors()).isEmpty();

        verify(messageSource).getMessage(eq("common.validation.error"), isNull(), any(Locale.class));
    }

    @Test
    @DisplayName("Should handle multiple validation errors for same field")
    void shouldHandleMultipleValidationErrorsForSameField() throws NoSuchMethodException {
        // Given
        FieldError fieldError1 = new FieldError("user", "email", "must be a valid email");
        FieldError fieldError2 = new FieldError("user", "email", "must not be blank");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // Create a real MethodParameter from a real method to avoid NullPointerException
        MethodParameter methodParameter = new MethodParameter(
            this.getClass().getDeclaredMethod("dummyMethod", String.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
            methodParameter, bindingResult);

        when(messageSource.getMessage(eq("common.validation.error"), isNull(), any(Locale.class)))
            .thenReturn("Validation failed");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleValidationExceptions(exception, webRequest);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getValidationErrors()).containsKey("email");
        // The last error message should be stored
        assertThat(response.getBody().getValidationErrors().get("email"))
            .isEqualTo("must not be blank");
    }

    @Test
    @DisplayName("Should use locale from LocaleContextHolder")
    void shouldUseLocaleFromContext() {
        // Given
        LocaleContextHolder.setLocale(Locale.FRENCH);
        AuthenticationException exception = new AuthenticationException("Bad credentials") {};

        when(messageSource.getMessage(eq("error.unauthorized"), isNull(), eq(Locale.FRENCH)))
            .thenReturn("Accès non autorisé");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleAuthenticationException(exception, webRequest);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Accès non autorisé");

        verify(messageSource).getMessage(eq("error.unauthorized"), isNull(), eq(Locale.FRENCH));

        // Reset locale
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    /**
     * Dummy method used to create a real MethodParameter for testing.
     * This avoids NullPointerException when creating MethodArgumentNotValidException.
     */
    @SuppressWarnings("unused")
    private void dummyMethod(String param) {
        // This method is only used for reflection in tests
    }
}
