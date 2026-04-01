package com.truong.spring.security.jwt.controller;

import com.truong.spring.security.jwt.model.payload.ApiResponse;
import com.truong.spring.security.jwt.security.JwtTokenProvider;
import com.truong.spring.security.jwt.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

  @Mock
  private AuthService authService;

  @Mock
  private JwtTokenProvider tokenProvider;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  private AuthController authController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    authController = new AuthController(authService, tokenProvider, applicationEventPublisher);
  }

  @Test
  @DisplayName("checkEmailInUse should return true when email exists")
  void checkEmailInUse_WhenEmailExists_ReturnsTrue() {
    // Arrange
    String email = "existing@example.com";
    when(authService.emailAlreadyExists(email)).thenReturn(true);

    // Act
    ResponseEntity<ApiResponse> response = authController.checkEmailInUse(email);

    // Assert
    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().getSuccess());
    assertEquals("true", response.getBody().getData());
    verify(authService, times(1)).emailAlreadyExists(email);
  }

  @Test
  @DisplayName("checkEmailInUse should return false when email does not exist")
  void checkEmailInUse_WhenEmailDoesNotExist_ReturnsFalse() {
    // Arrange
    String email = "new@example.com";
    when(authService.emailAlreadyExists(email)).thenReturn(false);

    // Act
    ResponseEntity<ApiResponse> response = authController.checkEmailInUse(email);

    // Assert
    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().getSuccess());
    assertEquals("false", response.getBody().getData());
    verify(authService, times(1)).emailAlreadyExists(email);
  }

  @Test
  @DisplayName("checkEmailInUse should handle null email parameter")
  void checkEmailInUse_WithNullEmail_ReturnsFalse() {
    // Arrange
    when(authService.emailAlreadyExists(null)).thenReturn(false);

    // Act
    ResponseEntity<ApiResponse> response = authController.checkEmailInUse(null);

    // Assert
    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().getSuccess());
    assertEquals("false", response.getBody().getData());
    verify(authService, times(1)).emailAlreadyExists(null);
  }

  @Test
  @DisplayName("checkEmailInUse should handle empty email string")
  void checkEmailInUse_WithEmptyEmail_ReturnsFalse() {
    // Arrange
    String email = "";
    when(authService.emailAlreadyExists(email)).thenReturn(false);

    // Act
    ResponseEntity<ApiResponse> response = authController.checkEmailInUse(email);

    // Assert
    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().getSuccess());
    assertEquals("false", response.getBody().getData());
    verify(authService, times(1)).emailAlreadyExists(email);
  }
}
