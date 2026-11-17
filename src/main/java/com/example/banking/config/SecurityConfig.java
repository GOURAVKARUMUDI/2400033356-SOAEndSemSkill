package com.example.banking.config;

import com.example.banking.repository.BankUserRepository;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.UUID;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecretKey jwtSecret(@Value("${app.jwt.secret-base64}") String secretBase64) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(secretBase64); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("JWT secret must be Base64 encoded", e); }
        if (bytes.length < 32) throw new IllegalStateException("JWT key must contain at least 32 random bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean JwtEncoder jwtEncoder(SecretKey jwtSecret) { return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecret)); }

    @Bean JwtDecoder jwtDecoder(SecretKey jwtSecret, BankUserRepository users,
                               @Value("${app.jwt.issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecret).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> standard = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains("banking-api")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        OAuth2TokenValidator<Jwt> activeAccount = jwt -> {
            try {
                UUID id = UUID.fromString(jwt.getSubject());
                long version = ((Number) jwt.getClaim("ver")).longValue();
                return users.findById(id).filter(u -> u.isEnabled() && u.getTokenVersion() == version)
                    .map(u -> OAuth2TokenValidatorResult.success())
                    .orElseGet(() -> OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Account disabled or token revoked", null)));
            } catch (RuntimeException e) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid subject/version", null));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(standard, audience, activeAccount));
        return decoder;
    }

    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // Bearer tokens only; do not use cookie auth in this setup.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
            .build();
    }
}
