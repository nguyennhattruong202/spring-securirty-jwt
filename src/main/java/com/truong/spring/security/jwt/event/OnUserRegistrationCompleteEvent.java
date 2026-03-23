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
package com.truong.spring.security.jwt.event;

import com.truong.spring.security.jwt.model.User;
//import lombok.Getter;
//import lombok.Setter;
import com.truong.spring.security.jwt.util.Util;
import org.springframework.context.ApplicationEvent;
//import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

//@Getter
//@Setter
public class OnUserRegistrationCompleteEvent extends ApplicationEvent {

//    private transient UriComponentsBuilder redirectUrl;
//    private User user;
//
//    public OnUserRegistrationCompleteEvent(User user, UriComponentsBuilder redirectUrl) {
//        super(user);
//        this.user = user;
//        this.redirectUrl = redirectUrl;
//    }
private final RegistrationEventData registrationEventData;

    public OnUserRegistrationCompleteEvent(User user, String verificationUrlBase) {
        super(user);
        this.registrationEventData = new RegistrationEventData(user, verificationUrlBase, Instant.now());
    }

    public RegistrationEventData getRegistrationEventData() {
        return registrationEventData;
    }

    public record RegistrationEventData(
            User user,
            String email,
            String verificationUrlBase,
            Instant timestamp,
            String eventId
    ) {
        public RegistrationEventData {
            if (user == null) {
                throw new IllegalArgumentException("User cannot be null");
            }
            if (verificationUrlBase == null || verificationUrlBase.isBlank()) {
                throw new IllegalArgumentException("Verification URL cannot be null or empty");
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }

        public RegistrationEventData(User user, String verificationUrlBase, Instant timestamp) {
            this(
                    user,
                    user.getEmail(),
                    verificationUrlBase,
                    timestamp,
                    Util.generateRandomUuid()
            );
        }

        public String getFullVerificationUrl(String token) {
            return verificationUrlBase + "?token=" + token + "&event=" + eventId;
        }

        public boolean isExpired() {
            return timestamp.plusSeconds(86400).isBefore(Instant.now()); // 24 hours expiry
        }
    }

    public User getUser() {
        return registrationEventData.user();
    }

    public String getEmail() {
        return registrationEventData.email();
    }

    public String getVerificationUrlBase() {
        return registrationEventData.verificationUrlBase();
    }
}
