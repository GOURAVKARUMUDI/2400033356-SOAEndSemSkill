package com.example.banking.dto;

import com.example.banking.model.BankUser;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String fullName, String email, Instant createdAt) {
    public static UserResponse from(BankUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt());
    }
}
