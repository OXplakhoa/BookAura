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
        BE->>DB: revoke old session and insert new session (same family_id)
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

Implemented details:
- Google client registration bean exists only when both credentials are non-blank; no-secret local/test startup remains valid.
- Spring Security validates OIDC state, nonce, signature and `email_verified`; callback code TTL is 60 seconds.
- `oauth_identities(provider, provider_subject)` links stable provider identity separately from mutable email.
- Existing matching email is linked and marked verified; otherwise a USER/profile is created with an unguessable
  BCrypt placeholder (no usable password is invented).
- Redirect code is URL-safe, DB stores SHA-256 only, and an atomic conditional update permits one exchange.
- React removes `?code=` from browser history before POSTing it, then uses the normal memory access token + HttpOnly refresh cookie.
- A transient `JSESSIONID` stores OAuth authorization state only and is invalidated by success/failure handlers.

## Change registered email (P0-B)

```mermaid
sequenceDiagram
    participant FE as React Account Settings
    participant BE as Spring Boot
    participant DB as PostgreSQL
    participant M as EmailSender

    FE->>BE: POST /api/account/email-change/request {newEmail} + Bearer
    BE->>DB: ensure new email is unique, then insert CHANGE_EMAIL SHA-256 code (10m)
    BE->>M: send raw six-digit code to new email
    Note over DB: Current account email is unchanged
    FE->>BE: POST /api/account/email-change/confirm {code} + Bearer
    BE->>DB: latest token for authenticated user/purpose, atomically consume it
    BE->>DB: update user_accounts.email + email_verified_at
    BE-->>FE: refreshed UserSummary
```

Wrong code hashes are constant-time compared. Each failed attempt is recorded in `REQUIRES_NEW`, because the
outer request deliberately throws 400 and would otherwise roll the counter back. Five failures lock the token;
resend cooldown is 60 seconds. Confirmation rechecks the unique email immediately before flush, and duplicate
races return 409. The existing JWT remains valid because identity is the immutable UUID `sub`, not email.

## Phone OTP login (mocked delivery)

- Public request response is identical for missing, disabled and active numbers; repeated active requests inside
  60 seconds also return the generic 200 while sending only once.
- `PHONE_LOGIN` code TTL is five minutes and reuses the same user/purpose binding, SHA-256, constant-time
  comparison, committed five-attempt lockout and atomic single-use consumption.
- Successful confirmation issues the normal access JWT + rotating refresh cookie; phone possession is the proof,
  so an otherwise active account does not require prior email verification for this alternate login.
- `FakeSmsSender` exists only in local/test, stores raw codes in process memory and never logs them. Local demo
  retrieval is an ADMIN-authorized, local-profile-only outbox; HTTP response redaction hides `phone` and `code`.
- Non-local/test profile without configured Brevo credentials uses `UnavailableSmsSender` and clearly reports that a real gateway is not configured.

## Phone OTP login (credential-conditional Brevo delivery)

The business flow is unchanged when delivery is real: `PhoneOtpService.request` normalizes the registered
phone, creates a five-minute `PHONE_LOGIN` token and sends the generated six-digit code through `SmsSender`.
The service method is transactional, so a known delivery failure rolls the new token row back and the user can
retry rather than receiving a misleading usable code.

Sender selection is explicit and mutually exclusive:

| Environment/configuration | Sender | Behavior |
|---|---|---|
| `local` or `test` | `FakeSmsSender` | In-memory delivery only; no Brevo request |
| non-local/test + `SMS_PROVIDER=brevo` + nonblank `BREVO_SMS_API_KEY` | `BrevoSmsSender` | `POST https://api.brevo.com/v3/transactionalSMS/sms` |
| other non-local/test configuration | `UnavailableSmsSender` | Existing `SMS_DELIVERY_UNAVAILABLE` response |

Brevo receives `api-key`, configured sender, normalized recipient and the transactional OTP message. Provider
4xx/5xx responses, timeouts and network failures map to the safe application error; raw phone, OTP, API key
and provider response bodies are not logged or returned. Brevo SMS is prepaid; this repository includes no live
credential and makes no live-delivery claim.
