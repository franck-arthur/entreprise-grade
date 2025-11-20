package com.enterprise.app.application.dto;

import com.enterprise.app.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for updating an existing user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Email(message = "{validation.user.email.invalid}")
    private String email;

    @Size(max = 50, message = "{validation.user.firstname.size}")
    private String firstName;

    @Size(max = 50, message = "{validation.user.lastname.size}")
    private String lastName;

    @Size(max = 20, message = "{validation.user.phone.size}")
    private String phoneNumber;

    private Boolean active;

    private Set<Role> roles;
}
