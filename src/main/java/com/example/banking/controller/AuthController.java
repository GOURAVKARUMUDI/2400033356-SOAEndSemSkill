package com.example.banking.controller;

import com.example.banking.dto.*;
import com.example.banking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/auth/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(input));
    }
    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest input) { return service.login(input); }

    @GetMapping("/users/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.me(UUID.fromString(jwt.getSubject()));
    }
    @GetMapping("/auth/validate")
    public Map<String, Object> validate(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("valid", true, "userId", jwt.getSubject(), "expiresAt", jwt.getExpiresAt().toString());
    }
    @PostMapping("/auth/logout-all")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        service.logoutAll(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/users/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest input) {
        service.changePassword(UUID.fromString(jwt.getSubject()), input);
        return ResponseEntity.noContent().build();
    }
}
