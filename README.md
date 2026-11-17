# Banking Token Authentication API

A Spring Boot REST API implementing JWT (HMAC-SHA256) bearer-token authentication for a banking application.

## Tech Stack

- **Java 21** with Spring Boot 4.1.2
- **Spring Security** with OAuth2 Resource Server for JWT validation
- **H2** in-memory database with JPA/Hibernate
- **BCrypt** password hashing (cost factor 12)
- **Jakarta Bean Validation** for request validation

## Features

- User registration and login with JWT token issuance
- Token validation endpoint
- Stateless session management (bearer tokens only, no cookies)
- Token revocation via version numbering (`tokenVersion` field)
- Logout-all-sessions support (invalidates all issued tokens)
- Password change with automatic token revocation
- Global exception handling

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Register a new user |
| POST | `/api/auth/login` | No | Login and receive JWT |
| GET | `/api/auth/validate` | Yes | Validate current token |
| GET | `/api/auth/me` | Yes | Get current user profile |
| POST | `/api/auth/logout-all` | Yes | Revoke all tokens |
| PUT | `/api/auth/change-password` | Yes | Change password & revoke tokens |

## Running the Application

```bash
# Set the JWT secret (Base64-encoded, minimum 32 bytes)
export APP_JWT_SECRET_BASE64=$(openssl rand -base64 32)

# Build and run
./mvnw spring-boot:run
```

The server starts on `http://localhost:8080`.

## Example Usage

```bash
# Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Alice","email":"alice@example.com","password":"Secret123!"}'

# Login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"Secret123!"}'

# Use the returned token for authenticated requests
curl -s http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <token>"
```

## Security Design

- **JWT (HMAC-SHA256)**: Tokens are signed with a shared secret and validated on every request.
- **Token Version**: Each user has a `tokenVersion` counter. Changing password or logging out increments the version, instantly invalidating all previously issued tokens.
- **BCrypt (cost 12)**: Passwords are hashed before storage.
- **Stateless**: No server-side session; all state lives in the JWT claims.

## Author

Gourav Karumudi — 2400033356
