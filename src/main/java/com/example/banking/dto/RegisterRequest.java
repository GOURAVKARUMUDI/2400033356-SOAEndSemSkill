package com.example.banking.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(min=2, max=100) String fullName,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=12, max=128) String password
) {}
