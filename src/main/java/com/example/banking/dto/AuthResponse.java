package com.example.banking.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {}
