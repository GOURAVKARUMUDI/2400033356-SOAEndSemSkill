package com.example.banking.service;

import com.example.banking.dto.*;
import com.example.banking.model.BankUser;
import com.example.banking.repository.BankUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final BankUserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String issuer;
    private final long lifetime;

    public AuthService(BankUserRepository users, PasswordEncoder passwords, JwtEncoder encoder,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.ttl-minutes:15}") long ttlMinutes) {
        this.users = users; this.passwords = passwords; this.encoder = encoder;
        this.issuer = issuer; this.lifetime = ttlMinutes * 60;
        if (ttlMinutes < 1 || ttlMinutes > 60) throw new IllegalArgumentException("JWT TTL must be 1–60 minutes");
    }

    @Transactional
    public UserResponse register(RegisterRequest input) {
        String email = normalize(input.email());
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        var user = new BankUser(input.fullName().trim(), email, passwords.encode(input.password()));
        try { return UserResponse.from(users.saveAndFlush(user)); }
        catch (DataIntegrityViolationException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered"); }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest input) {
        BankUser user = users.findByEmail(normalize(input.email()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.isEnabled() || !passwords.matches(input.password(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer)
            .subject(user.getId().toString()).audience(List.of("banking-api"))
            .issuedAt(now).expiresAt(now.plus(lifetime, ChronoUnit.SECONDS))
            .id(UUID.randomUUID().toString()).claim("ver", user.getTokenVersion()).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", lifetime, UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID id) { return UserResponse.from(requireUser(id)); }

    @Transactional
    public void logoutAll(UUID id) { requireUser(id).revokeTokens(); }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest input) {
        BankUser user = requireUser(id);
        if (!passwords.matches(input.currentPassword(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect current password");
        if (passwords.matches(input.newPassword(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different");
        user.setPasswordHash(passwords.encode(input.newPassword()));
        user.revokeTokens();
    }
    private BankUser requireUser(UUID id) {
        return users.findById(id).filter(BankUser::isEnabled)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid account"));
    }
    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
