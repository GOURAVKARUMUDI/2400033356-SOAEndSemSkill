package com.example.banking.dto;

import jakarta.validation.constraints.*;

public record ChangePasswordRequest(@NotBlank String currentPassword,
                                    @NotBlank @Size(min=12, max=128) String newPassword) {}
