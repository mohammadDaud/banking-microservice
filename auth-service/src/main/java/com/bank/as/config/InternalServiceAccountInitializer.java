package com.bank.as.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalServiceAccountInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {

        log.info("Initializing internal service account...");

        createRole();
        createTransactionServiceUser();
        assignRole();

        log.info("Internal service account initialization completed.");
    }

    private void createRole() {
        jdbcTemplate.update("""
            INSERT INTO roles (id, role_name)
            SELECT
                gen_random_uuid()::text,
                'ROLE_INTERNAL_SERVICE'
            WHERE NOT EXISTS (
                SELECT 1
                FROM roles
                WHERE role_name = 'ROLE_INTERNAL_SERVICE'
            )
        """);
    }

    private void createTransactionServiceUser() {
        jdbcTemplate.update("""
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
            )
        """);
    }

    private void assignRole() {
        jdbcTemplate.update("""
            INSERT INTO user_roles (user_id, role_id)
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
              )
        """);
    }
}