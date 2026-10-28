package com.example.demo.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsApplicationErrorsToTheirHttpStatuses() {
        assertEquals(404, handler.handleResourceNotFoundException(
                new ResourceNotFoundException("Customer", 8L)).getStatusCode().value());
        assertEquals(400, handler.handleBusinessException(
                new BusinessException("Invalid order")).getStatusCode().value());
        assertEquals(500, handler.handleGlobalException(
                new IllegalStateException("internal detail")).getStatusCode().value());
    }

    @Test
    void mapsMissingStaticResourcesToNotFound() {
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/missing");

        assertEquals(404, handler.handleNoResourceFoundException(exception).getStatusCode().value());
        assertEquals("Resource not found",
                handler.handleNoResourceFoundException(exception).getBody().get("message"));
    }

    @Test
    void mapsInvalidArgumentsToFieldErrors() throws Exception {
        var bindingResult = new MapBindingResult(new HashMap<String, Object>(), "request");
        bindingResult.rejectValue("email", "invalid", "Email is invalid");
        var method = getClass().getDeclaredMethod("accepts", Object.class);
        var exception = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(method, 0), bindingResult);

        var response = handler.handleValidationExceptions(exception);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(((Map<?, ?>) response.getBody().get("errors")).containsKey("email"));
    }

    private void accepts(Object value) {
    }
}
