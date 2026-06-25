/*
 * Technical service account for Transaction Service.
 *
 * This account must never use:
 * - /api/auth/register
 * - /api/auth/login
 * - OTP
 * - refresh token
 *
 * It is used only by:
 * POST /api/auth/internal/token
 */

-- 1. Create internal service role
INSERT INTO roles (
    id,
    role_name
)
SELECT
    gen_random_uuid()::text,
    'ROLE_INTERNAL_SERVICE'
    WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE role_name = 'ROLE_INTERNAL_SERVICE'
);


-- 2. Create Transaction Service technical user
--
-- password is intentionally a BCrypt hash of a random unused password.
-- This account is blocked from normal login in LoginService.
--
-- Do NOT use this password for clientSecret.
-- clientSecret is separately stored as BCrypt hash in environment variable.
INSERT INTO users (
    id,
    username,
    email,
    password,
    enabled,
    failed_attempts,
    account_locked,
    email_verified,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid()::text,
    'transaction-service',
    'transaction-service@internal.bank',
    '$2a$10$4A8X0DmbzDldO6FiN0OeOeYeGvP8VvL5m6DqkZ8fYQbq2mQJ0mL2G',
    TRUE,
    0,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE username = 'transaction-service'
);


-- 3. Assign only ROLE_INTERNAL_SERVICE to transaction-service
INSERT INTO user_roles (
    user_id,
    role_id
)
SELECT
    u.id,
    r.id
FROM users u
         JOIN roles r
              ON r.role_name = 'ROLE_INTERNAL_SERVICE'
WHERE u.username = 'transaction-service'
  AND NOT EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id
      AND ur.role_id = r.id
);