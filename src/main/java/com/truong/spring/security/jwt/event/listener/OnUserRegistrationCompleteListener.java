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
package com.truong.spring.security.jwt.event.listener;

import com.truong.spring.security.jwt.event.OnUserRegistrationCompleteEvent;
import com.truong.spring.security.jwt.exception.MailSendException;
import com.truong.spring.security.jwt.model.User;
import com.truong.spring.security.jwt.service.EmailVerificationTokenService;
import com.truong.spring.security.jwt.service.MailService;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OnUserRegistrationCompleteListener {
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final MailService mailService;
    @Value("${app.verification.url.validation.enabled:true}")
    private boolean validateUrl;

    /**
     * As soon as a registration event is complete, invoke the email verification
     * asynchronously in another thread pool
     * Send email verification to the user and persist the token in the database.
     */
    @Async("taskExecutor")
    @EventListener
    @Retryable(
            retryFor = {MailSendException.class, MessagingException.class, IOException.class, TemplateException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendEmailVerification(OnUserRegistrationCompleteEvent event) {
        final var data = event.getRegistrationEventData();
        log.info("Processing verification for eventId: {}", data.eventId());

        if (data.isExpired()) {
            log.warn("Event {} expired, skipping email", data.eventId());
            return;
        }

        try {
            User user = data.user();
            String token = emailVerificationTokenService.generateNewToken();
            if (token == null || token.isBlank()) {
                log.error("Generated token is null or empty for user: {}", user.getUserId());
                return;
            }
            try {
                emailVerificationTokenService.createVerificationToken(user, token);
            } catch (DataIntegrityViolationException e) {
                log.error("Token already exists for user: {}", user.getUserId(), e);
                return;
            }

            String emailConfirmationUrl = data.getFullVerificationUrl(token);
            if (validateUrl && !isValidUrl(emailConfirmationUrl)) {
                log.error("Invalid confirmation URL for user: {}, URL: {}", user.getUserId(), emailConfirmationUrl);
                return;
            }

            String recipientAddress = data.email();
            mailService.sendEmailVerification(emailConfirmationUrl, recipientAddress);
            log.info("Verification email sent successfully to {} for eventId: {}", recipientAddress, data.eventId());
        } catch (IOException | TemplateException | MessagingException e) {
            log.error("Failed to send verification email for eventId: {}", data.eventId(), e);
            throw new MailSendException(data.email(), "Email Verification", e);
        }
    }

    @Recover
    public void recover(MailSendException e, OnUserRegistrationCompleteEvent event) {
        var data = event.getRegistrationEventData();
        log.error("Failed to send email after retries for eventId: {}, email: {}",
                data.eventId(), data.email(), e);
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            var validatedUrl = java.net.URI.create(url).toURL();
            return true;
        } catch (IllegalArgumentException | java.net.MalformedURLException e) {
            return false;
        }
    }
}
