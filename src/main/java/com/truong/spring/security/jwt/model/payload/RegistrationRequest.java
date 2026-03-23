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
package com.truong.spring.security.jwt.model.payload;

import com.truong.spring.security.jwt.validation.annotation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(name = "Registration Request", description = "The registration request payload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequest {

    @NullOrNotBlank(message = "Registration username can be null but not blank")
    @Schema(name = "A valid username", allowableValues = "NonEmpty String")
    private String username;

    @NullOrNotBlank(message = "Registration email can be null but not blank")
    @Schema(name = "A valid email", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = "NonEmpty String")
    private String email;

    @NotNull(message = "Registration password cannot be null")
    @Schema(name = "A valid password string", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = "NonEmpty String")
    private String password;

    @NotNull(message = "Specify whether the user has to be registered as an admin or not")
    @Schema(name = "Flag denoting whether the user is an admin or not",
            requiredMode = Schema.RequiredMode.REQUIRED, type = "boolean", allowableValues = "true, false")
    private Boolean registerAsAdmin;
}
