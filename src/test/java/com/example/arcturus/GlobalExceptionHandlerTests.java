package com.example.arcturus;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationExceptionReturnsBadRequestWithAllFieldErrors() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new NotificationRequest(), "notificationRequest");
        bindingResult.addError(new FieldError(
                "notificationRequest", "type", "", false, null, null, "Notification type is required"));
        bindingResult.addError(new FieldError(
                "notificationRequest", "target", "", false, null, null, "Target is required"));
        Method controllerMethod =
                NotificationController.class.getMethod("sendNotification", NotificationRequest.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new MethodParameter(controllerMethod, 0), bindingResult);

        var response = handler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Validation failed", response.getBody().getMessage());
        assertEquals(
                "type: Notification type is required, target: Target is required",
                response.getBody().getDetails());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void sendNotificationReturnsBadRequestWhenRequestValidationFails() throws Exception {
        NotificationService service = mock(NotificationService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new NotificationController(service))
                .setControllerAdvice(handler)
                .build();

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "",
                                  "target": "",
                                  "payload": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").value(containsString("type: Notification type is required")));

        verifyNoInteractions(service);
    }

    @Test
    void handleIllegalArgumentExceptionReturnsBadRequestWithExceptionMessage() {
        var response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Notification type cannot be blank"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Invalid argument", response.getBody().getMessage());
        assertEquals("Notification type cannot be blank", response.getBody().getDetails());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleGenericExceptionReturnsInternalServerErrorWithExceptionMessage() {
        var response = handler.handleGenericException(new Exception("Unexpected failure"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertEquals("Unexpected failure", response.getBody().getDetails());
        assertNotNull(response.getBody().getTimestamp());
    }
}
