package com.enterprise.app.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a parsed CSV line for user import.
 * Expected CSV format: username,email,firstName,lastName,phoneNumber,roles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvUserLine {
    private int lineNumber;
    private String rawLine;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String roles; // Comma-separated roles: ADMIN,USER
}
