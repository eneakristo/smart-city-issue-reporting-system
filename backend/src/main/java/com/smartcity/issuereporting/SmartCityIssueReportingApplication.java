package com.smartcity.issuereporting;

import java.net.URI;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
public class SmartCityIssueReportingApplication {

    private static final Logger log = LoggerFactory.getLogger(SmartCityIssueReportingApplication.class);

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(SmartCityIssueReportingApplication.class);
        application.addListeners(SmartCityIssueReportingApplication::logDatabaseConfiguration);
        application.run(args);
    }

    private static void logDatabaseConfiguration(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        String datasourceUrl = environment.getProperty("spring.datasource.url", "");
        String datasourceUsername = environment.getProperty("spring.datasource.username", "");
        String activeProfiles = Arrays.toString(environment.getActiveProfiles());

        log.info(
                "Database configuration: profiles={}, url={}, username={}",
                activeProfiles,
                describeJdbcUrl(datasourceUrl),
                datasourceUsername
        );
    }

    private static String describeJdbcUrl(String datasourceUrl) {
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            return "<empty>";
        }

        try {
            URI uri = URI.create(datasourceUrl.replaceFirst("^jdbc:", ""));
            int port = uri.getPort();
            String database = uri.getPath() == null || uri.getPath().isBlank() ? "" : uri.getPath();

            return "host=" + uri.getHost()
                    + ", port=" + (port == -1 ? "<default>" : port)
                    + ", database=" + database;
        } catch (IllegalArgumentException ex) {
            return "<unparseable>";
        }
    }
}
