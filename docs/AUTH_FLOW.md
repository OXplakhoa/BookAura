# AUTH FLOWS

## Registration + email verification

```mermaid
sequenceDiagram
    participant U as User
    participant FE as React SPA
    participant BE as Spring Boot
    participant DB as PostgreSQL
    participant M as Mailpit/Brevo

    U->>FE: register(email, password)
    FE->>BE: POST /api/auth/register
    BE->>DB: insert user_accounts (email_verified_at = NULL)
    BE->>DB: insert otp_tokens (purpose=EMAIL_VERIFICATION, code_hash)
    BE->>M: send verification link (frontend /verify-email?token=...)
    U->>FE: clicks link
    FE->>BE: POST /api/auth/verify-email {token}
    BE->>DB: hash(token) match + not expired + not consumed → set email_verified_at, consumed_at
    Note over U,BE: login before verification → 401 EMAIL_NOT_VERIFIED
```

## Login (email or normalized phone + password)

```mermaid
sequenceDiagram
    participant FE as React SPA
    participant BE as Spring Boot
    participant DB as PostgreSQL

    FE->>BE: POST /api/auth/login {identifier, password}
    BE->>DB: find by lower(email) OR phone
    BE->>BE: BCrypt matches? verified? status=ACTIVE?
    BE->>DB: insert refresh_sessions (family_id=new, token_hash)
    BE-->>FE: 200 {accessToken(15m), user} + Set-Cookie refreshToken (HttpOnly, 7d)
    Note over FE: accessToken kept in MEMORY only (never localStorage)
```

## Refresh rotation + reuse detection

```mermaid
sequenceDiagram
    participant FE as React SPA
    participant BE as Spring Boot
    participant DB as PostgreSQL

    FE->>BE: POST /api/auth/refresh (cookie)
    BE->>DB: find refresh_sessions by hash(token)
    alt valid & active & not expired
        BE->>DB: revoke old session; insert new session (same family_id)
        BE-->>FE: new accessToken + rotated cookie
    else revoked/expired token presented (reuse)
        BE->>DB: revoke ALL sessions in family_id
        BE-->>FE: 401 + clear cookie
    end
```

Silent refresh: on SPA load/401-expiry, React Query calls `/refresh` once (single-flight) and retries.

## Logout

```mermaid
sequenceDiagram
    participant FE as React SPA
    participant BE as Spring Boot
    participant DB as PostgreSQL

    FE->>BE: POST /api/auth/logout (Bearer accessToken + cookie)
    BE->>DB: insert revoked_access_tokens (jti, exp of the access token)
    BE->>DB: revoke refresh session from cookie
    BE-->>FE: 204 + clear cookie
    Note over BE: JwtAuthenticationFilter rejects revoked jti → old access token = 401
```

## Token storage decisions (D4)

| Token | Where | Why |
|-------|-------|-----|
| Access | JS memory (React state/query cache) | XSS in localStorage exfiltrates tokens; memory dies with tab. Trade-off: F5 loses it → silent refresh. |
| Refresh | `HttpOnly; Secure(prod); SameSite=Lax; Path=/api/auth` cookie | JS cannot read it (XSS-safe); browser sends it only to refresh endpoints. |

- **CSRF**: refresh/logout are POST + `SameSite=Lax` (cross-site POSTs don't carry the cookie) + strict CORS
  allowlist (`FRONTEND_URL`, `allowCredentials=true`). If SameSite is ever relaxed, add CSRF tokens.
- **CORS**: single explicit origin, credentials allowed, no `*`.
- **401 vs 403**: 401 = not authenticated (bad/expired/revoked token, bad credentials, unverified email with
  code `EMAIL_NOT_VERIFIED`); 403 = authenticated but lacks role. Consistent error JSON everywhere.

## OAuth (P0-B, Google first)

```mermaid
sequenceDiagram
    participant FE as React SPA
    participant BE as Spring Boot
    participant G as Google

    FE->>BE: GET /oauth2/authorization/google
    BE->>G: redirect to Google consent
    G->>BE: GET /login/oauth2/code/google (backend callback)
    BE->>BE: find-or-create/link UserAccount (verified by provider)
    BE->>BE: create one-time authorization code (TTL 60s, single use)
    BE-->>FE: redirect FRONTEND_URL/oauth/callback?code=...
    FE->>BE: POST /api/auth/oauth/exchange {code}
    BE-->>FE: accessToken + refresh cookie (same session model as password login)
```

**Never** place JWTs in the redirect URL query params.
