package com.enterprise.app.application.dto;

import com.enterprise.app.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for creating a new user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "{validation.user.username.required}")
    @Size(min = 3, max = 50, message = "{validation.user.username.size}")
    private String username;

    @NotBlank(message = "{validation.user.email.required}")
    @Email(message = "{validation.user.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.user.password.required}")
    @Size(min = 8, message = "{validation.user.password.size}")
    private String password;

    @Size(max = 50, message = "{validation.user.firstname.size}")
    private String firstName;

    @Size(max = 50, message = "{validation.user.lastname.size}")
    private String lastName;

    @Size(max = 20, message = "{validation.user.phone.size}")
    private String phoneNumber;

    private Set<Role> roles;
}
