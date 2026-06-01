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
        jdbcTemplate.update("UPDATE users SET active = TRUE WHERE active IS NULL");
        jdbcTemplate.update("UPDATE users SET role = 'ADMINISTRATOR' WHERE role = 'ADMIN'");
        jdbcTemplate.update("UPDATE users SET assigned_zone = 'ZONE_1' WHERE role = 'ADMINISTRATOR' AND assigned_zone IS NULL");
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

        String systemAdminEmail = applicationProperties.getSystemAdmin().getBootstrapEmail();
        if (systemAdminEmail != null && !systemAdminEmail.isBlank()) {
            jdbcTemplate.update(
                    "UPDATE users SET role = 'SYSTEM_ADMIN' WHERE LOWER(email) = LOWER(?)",
                    systemAdminEmail.trim()
            );
        }
    }
}
