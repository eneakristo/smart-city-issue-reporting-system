package com.smartcity.issuereporting.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoleDataMigration implements ApplicationRunner {

    private final ApplicationProperties applicationProperties;
    private final JdbcTemplate jdbcTemplate;

    public RoleDataMigration(ApplicationProperties applicationProperties, JdbcTemplate jdbcTemplate) {
        this.applicationProperties = applicationProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        dropZoneCheckConstraints();
        jdbcTemplate.update("UPDATE users SET active = TRUE WHERE active IS NULL");
        jdbcTemplate.update("UPDATE users SET role = 'ADMINISTRATOR' WHERE role = 'ADMIN'");
        jdbcTemplate.update(
                """
                UPDATE users
                SET assigned_zone = 'ZONE_1'
                WHERE role = 'ADMINISTRATOR'
                  AND (
                      assigned_zone IS NULL
                      OR assigned_zone NOT IN ('ZONE_1', 'ZONE_2', 'ZONE_3', 'ZONE_4')
                  )
                """
        );
        jdbcTemplate.update(
                """
                UPDATE issues
                SET zone = CASE
                    WHEN latitude >= ? AND longitude < ? THEN 'ZONE_1'
                    WHEN latitude >= ? AND longitude >= ? THEN 'ZONE_2'
                    WHEN latitude < ? AND longitude < ? THEN 'ZONE_3'
                    ELSE 'ZONE_4'
                END
                WHERE latitude IS NOT NULL
                  AND longitude IS NOT NULL
                """,
                applicationProperties.getZoning().getSplitLatitude(),
                applicationProperties.getZoning().getSplitLongitude(),
                applicationProperties.getZoning().getSplitLatitude(),
                applicationProperties.getZoning().getSplitLongitude(),
                applicationProperties.getZoning().getSplitLatitude(),
                applicationProperties.getZoning().getSplitLongitude()
        );
        jdbcTemplate.update(
                """
                UPDATE issues
                SET zone = NULL
                WHERE zone IS NOT NULL
                  AND zone NOT IN ('ZONE_1', 'ZONE_2', 'ZONE_3', 'ZONE_4')
                """
        );

        String systemAdminEmail = applicationProperties.getSystemAdmin().getBootstrapEmail();
        if (systemAdminEmail != null && !systemAdminEmail.isBlank()) {
            jdbcTemplate.update(
                    "UPDATE users SET role = 'SYSTEM_ADMIN' WHERE LOWER(email) = LOWER(?)",
                    systemAdminEmail.trim()
            );
        }
        jdbcTemplate.update(
                """
                UPDATE users
                SET assigned_zone = NULL
                WHERE role <> 'ADMINISTRATOR'
                  AND assigned_zone IS NOT NULL
                  AND assigned_zone NOT IN ('ZONE_1', 'ZONE_2', 'ZONE_3', 'ZONE_4')
                """
        );
        applyZoneCheckConstraints();
    }

    private void dropZoneCheckConstraints() {
        jdbcTemplate.execute("ALTER TABLE issues DROP CONSTRAINT IF EXISTS issues_zone_check");
        jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_assigned_zone_check");
    }

    private void applyZoneCheckConstraints() {
        jdbcTemplate.execute(
                """
                ALTER TABLE issues
                ADD CONSTRAINT issues_zone_check
                CHECK (zone IS NULL OR zone IN ('ZONE_1', 'ZONE_2', 'ZONE_3', 'ZONE_4'))
                """
        );
        jdbcTemplate.execute(
                """
                ALTER TABLE users
                ADD CONSTRAINT users_assigned_zone_check
                CHECK (assigned_zone IS NULL OR assigned_zone IN ('ZONE_1', 'ZONE_2', 'ZONE_3', 'ZONE_4'))
                """
        );
    }
}
