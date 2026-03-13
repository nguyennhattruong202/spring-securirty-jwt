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
import com.truong.spring.security.jwt.model.User;
import com.truong.spring.security.jwt.service.EmailVerificationTokenService;
import com.truong.spring.security.jwt.service.MailService;
import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OnUserRegistrationCompleteListener {
    private final EmailVerificationTokenService emailVerificationTokenService;
    private final MailService mailService;

    /**
     * As soon as a registration event is complete, invoke the email verification
     * asynchronously in another thread pool
     * Send email verification to the user and persist the token in the database.
     */
    @Async("taskExecutor")
    @EventListener
    private void sendEmailVerification(OnUserRegistrationCompleteEvent event) {
        var data = event.getRegistrationEventData();
        log.info("Processing verification for eventId: {}", data.eventId());
        try {
            User user = data.user();
            String token = emailVerificationTokenService.generateNewToken();
            emailVerificationTokenService.createVerificationToken(user, token);
            String recipientAddress = data.email();
            String emailConfirmationUrl = data.getFullVerificationUrl(token);
            mailService.sendEmailVerification(emailConfirmationUrl, recipientAddress);
            log.info("Verification email sent to {} for eventId: {}", recipientAddress, data.eventId());
        } catch (IOException | TemplateException | MessagingException e) {
            log.error("Failed to send verification email for eventId: {}", data.eventId(), e);
        }
    }
}
