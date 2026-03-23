/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.truong.spring.security.jwt.controller;

import com.truong.spring.security.jwt.event.OnGenerateResetLinkEvent;
import com.truong.spring.security.jwt.event.OnRegenerateEmailVerificationEvent;
import com.truong.spring.security.jwt.event.OnUserAccountChangeEvent;
import com.truong.spring.security.jwt.event.OnUserRegistrationCompleteEvent;
import com.truong.spring.security.jwt.exception.*;
import com.truong.spring.security.jwt.model.CustomUserDetails;
import com.truong.spring.security.jwt.model.User;
import com.truong.spring.security.jwt.model.payload.*;
import com.truong.spring.security.jwt.model.token.EmailVerificationToken;
import com.truong.spring.security.jwt.model.token.RefreshToken;
import com.truong.spring.security.jwt.security.JwtTokenProvider;
import com.truong.spring.security.jwt.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authorization Rest API", description = "Defines endpoints that can be hit only when the user is not " +
        "logged in. It's not secured by default.")
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public AuthController(AuthService authService, JwtTokenProvider tokenProvider, ApplicationEventPublisher applicationEventPublisher) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Checks is a given email is in use or not.
     */
    @Operation(summary = "Checks if the given email is in use")
    @GetMapping("/checkEmailInUse")
    public ResponseEntity checkEmailInUse(@Param(value = "Email id to check against") @RequestParam("email") String email) {
        Boolean emailExists = authService.emailAlreadyExists(email);
        return ResponseEntity.ok(new ApiResponse(true, emailExists.toString()));
    }

    /**
     * Checks is a given username is in use or not.
     */
    @Operation(summary = "Checks if the given username is in use")
    @GetMapping("/checkUsernameInUse")
    public ResponseEntity checkUsernameInUse(@Param(value = "Username to check against") @RequestParam(
            "username") String username) {
        Boolean usernameExists = authService.usernameAlreadyExists(username);
        return ResponseEntity.ok(new ApiResponse(true, usernameExists.toString()));
    }


    /**
     * Entry point for the user log in. Return the jwt auth token and the refresh token
     */
    @PostMapping("/login")
    @Operation(summary = "Logs the user in to the system and return the auth tokens")
    public ResponseEntity authenticateUser(@Param(value = "The LoginRequest payload") @Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authService.authenticateUser(loginRequest)
                .orElseThrow(() -> new UserLoginException("Couldn't login user [" + loginRequest + "]"));

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        log.info("Logged in User returned [API]: " + customUserDetails.getUsername());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return authService.createAndPersistRefreshTokenForDevice(authentication, loginRequest)
                .map(RefreshToken::getToken)
                .map(refreshToken -> {
                    String jwtToken = authService.generateToken(customUserDetails);
                    return ResponseEntity.ok(new JwtAuthenticationResponse(jwtToken, refreshToken, tokenProvider.getExpiryDuration()));
                })
                .orElseThrow(() -> new UserLoginException("Couldn't create refresh token for: [" + loginRequest + "]"));
    }

    /**
     * Entry point for the user registration process. On successful registration,
     * publish an event to generate email verification token
     */
    @PostMapping("/register")
    @Operation(summary = "Registers the user and publishes an event to generate the email verification")
    public ResponseEntity<ApiResponse> registerUser(@Parameter(description = "The RegistrationRequest payload")
                                                    @Valid @RequestBody RegistrationRequest registrationRequest) {
      log.debug("Processing registration for email: {}", maskEmail(registrationRequest.getEmail()));
        return authService.registerUser(registrationRequest)
                .map(user -> {
                  String verificationUrl = buildVerificationUrl();
                  publishRegistrationEvent(user, verificationUrl);
                  log.info("User registered successfully with ID: {}", user.getUserId());
                  return ResponseEntity.ok(new ApiResponse(true,
                          "Registration successful. Please check your email to verify your account."));
                })
                .orElseThrow(() -> new UserRegistrationException(registrationRequest.getEmail(),
                        "Failed to register user: "));
    }

  private void publishRegistrationEvent(User user, String verificationUrl) {
    // Tạo event với user và base URL
    OnUserRegistrationCompleteEvent event =
            new OnUserRegistrationCompleteEvent(user, verificationUrl);
    // Publish event - Spring sẽ gửi đến tất cả listeners
    applicationEventPublisher.publishEvent(event);
    log.debug("Published registration event for user: {} with eventId: {}",
            user.getUserId(), event.getRegistrationEventData().eventId());
  }

  private String maskEmail(String email) {
    if (email == null || !email.contains("@")) return email;
    String[] parts = email.split("@");
    String localPart = parts[0];
    String domain = parts[1];

    if (localPart.length() <= 2) return email;
    return localPart.substring(0, 2) + "***@" + domain;
  }

  private String buildVerificationUrl() {
    return ServletUriComponentsBuilder
            .fromCurrentContextPath()
            .path("/api/auth/registrationConfirmation")
            .build()
            .toUriString();
  }

    /**
     * Receives the reset link request and publishes an event to send email id containing
     * the reset link if the request is valid. In future the deeplink should open within
     * the app itself.
     */
    @PostMapping("/password/resetlink")
    @Operation(summary = "Receive the reset link request and publish event to send mail containing the password " +
            "reset link")
    public ResponseEntity resetLink(@Param(value = "The PasswordResetLinkRequest payload") @Valid @RequestBody PasswordResetLinkRequest passwordResetLinkRequest) {

        return authService.generatePasswordResetToken(passwordResetLinkRequest)
                .map(passwordResetToken -> {
                    UriComponentsBuilder urlBuilder = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api" +
                            "/auth/password/reset");
                    OnGenerateResetLinkEvent generateResetLinkMailEvent =
                            new OnGenerateResetLinkEvent(passwordResetToken,
                            urlBuilder);
                    applicationEventPublisher.publishEvent(generateResetLinkMailEvent);
                    return ResponseEntity.ok(new ApiResponse(true, "Password reset link sent successfully"));
                })
                .orElseThrow(() -> new PasswordResetLinkException(passwordResetLinkRequest.getEmail(), "Couldn't " +
                        "create a valid token"));
    }

    /**
     * Receives a new passwordResetRequest and sends the acknowledgement after
     * changing the password to the user's mail through the event.
     */

    @PostMapping("/password/reset")
    @Operation(summary = "Reset the password after verification and publish an event to send the acknowledgement " +
            "email")
    public ResponseEntity resetPassword(@Param(value = "The PasswordResetRequest payload") @Valid @RequestBody PasswordResetRequest passwordResetRequest) {

        return authService.resetPassword(passwordResetRequest)
                .map(changedUser -> {
                    OnUserAccountChangeEvent onPasswordChangeEvent = new OnUserAccountChangeEvent(changedUser, "Reset" +
                            " Password",
                            "Changed Successfully");
                    applicationEventPublisher.publishEvent(onPasswordChangeEvent);
                    return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully"));
                })
                .orElseThrow(() -> new PasswordResetException(passwordResetRequest.getToken(), "Error in resetting " +
                        "password"));
    }

    /**
     * Confirm the email verification token generated for the user during
     * registration. If token is invalid or token is expired, report error.
     */
    @GetMapping("/registrationConfirmation")
    @Operation(summary = "Confirms the email verification token that has been generated for the user during " +
            "registration")
    public ResponseEntity confirmRegistration(@Param(value = "the token that was sent to the user email") @RequestParam("token") String token) {

        return authService.confirmEmailRegistration(token)
                .map(user -> ResponseEntity.ok(new ApiResponse(true, "User verified successfully")))
                .orElseThrow(() -> new InvalidTokenRequestException("Email Verification Token", token, "Failed to " +
                        "confirm. Please generate a new email verification request"));
    }

    /**
     * Resend the email registration mail with an updated token expiry. Safe to
     * assume that the user would always click on the last re-verification email and
     * any attempts at generating new token from past (possibly archived/deleted)
     * tokens should fail and report an exception.
     */
    @GetMapping("/resendRegistrationToken")
    @Operation(summary = "Resend the email registration with an updated token expiry. Safe to " +
            "assume that the user would always click on the last re-verification email and " +
            "any attempts at generating new token from past (possibly archived/deleted)" +
            "tokens should fail and report an exception. ")
    public ResponseEntity resendRegistrationToken(@Param(value = "the initial token that was sent to the user email " +
            "after registration") @RequestParam("token") String existingToken) {

        EmailVerificationToken newEmailToken = authService.recreateRegistrationToken(existingToken)
                .orElseThrow(() -> new InvalidTokenRequestException("Email Verification Token", existingToken, "User " +
                        "is already registered. No need to re-generate token"));

        return Optional.ofNullable(newEmailToken.getUser())
                .map(registeredUser -> {
                    UriComponentsBuilder urlBuilder = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api" +
                            "/auth/registrationConfirmation");
                    OnRegenerateEmailVerificationEvent regenerateEmailVerificationEvent = new OnRegenerateEmailVerificationEvent(registeredUser, urlBuilder, newEmailToken);
                    applicationEventPublisher.publishEvent(regenerateEmailVerificationEvent);
                    return ResponseEntity.ok(new ApiResponse(true, "Email verification resent successfully"));
                })
                .orElseThrow(() -> new InvalidTokenRequestException("Email Verification Token", existingToken, "No " +
                        "user associated with this request. Re-verification denied"));
    }

    /**
     * Refresh the expired jwt token using a refresh token for the specific device
     * and return a new token to the caller
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh the expired jwt authentication by issuing a token refresh request and returns the" +
            "updated response tokens")
    public ResponseEntity refreshJwtToken(@Param(value = "The TokenRefreshRequest payload") @Valid @RequestBody TokenRefreshRequest tokenRefreshRequest) {

        return authService.refreshJwtToken(tokenRefreshRequest)
                .map(updatedToken -> {
                    String refreshToken = tokenRefreshRequest.getRefreshToken();
                    log.info("Created new Jwt Auth token: " + updatedToken);
                    return ResponseEntity.ok(new JwtAuthenticationResponse(updatedToken, refreshToken, tokenProvider.getExpiryDuration()));
                })
                .orElseThrow(() -> new TokenRefreshException(tokenRefreshRequest.getRefreshToken(), "Unexpected error during token refresh. Please logout and login again."));
    }
}
