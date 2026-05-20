package com.smartcity.issuereporting.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private final Cors cors = new Cors();
    private final Jwt jwt = new Jwt();
    private final Storage storage = new Storage();
    private final Geocoding geocoding = new Geocoding();
    private final Zoning zoning = new Zoning();
    private final Prioritization prioritization = new Prioritization();
    private final SystemAdmin systemAdmin = new SystemAdmin();
    private final PasswordReset passwordReset = new PasswordReset();
    private final Email email = new Email();

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "change-this-secret-in-local-config";
        private long accessTokenExpirationMinutes = 60;
        private long refreshTokenExpirationDays = 7;
    }

    @Getter
    @Setter
    public static class Storage {
        private String supabaseUrl;
        private String supabaseServiceKey;
        private String supabaseBucket = "issue-image";
        private String issueImageObjectPrefix = "issues";
        private long maxImageFileSizeBytes = 5_242_880L;
        private List<String> allowedImageContentTypes = new ArrayList<>(List.of(
                "image/jpeg",
                "image/png",
                "image/webp"
        ));
    }

    @Getter
    @Setter
    public static class Geocoding {
        private boolean enabled = true;
        private String baseUrl = "https://nominatim.openstreetmap.org";
        private String searchPath = "/search";
        private String userAgent = "smart-city-issue-reporting/1.0";
        private String defaultQuerySuffix = "Tirana, Albania";
        private String countryCode = "al";
        private int resultLimit = 1;
    }

    @Getter
    @Setter
    public static class Zoning {
        private BigDecimal splitLongitude = BigDecimal.valueOf(19.8187d);
    }

    @Getter
    @Setter
    public static class Prioritization {
        private BigDecimal cityCenterLatitude = BigDecimal.valueOf(41.3275d);
        private BigDecimal cityCenterLongitude = BigDecimal.valueOf(19.8187d);
        private int voteCountForFullScore = 20;
        private int duplicateCountForFullScore = 5;
    }

    @Getter
    @Setter
    public static class SystemAdmin {
        private String bootstrapEmail;
    }

    @Getter
    @Setter
    public static class PasswordReset {
        private String frontendResetUrl = "http://localhost:5173/reset-password";
        private long tokenExpirationMinutes = 30;
    }

    @Getter
    @Setter
    public static class Email {
        private String provider = "brevo";
        private String brevoApiKey;
        private String fromAddress;
        private String fromName = "Smart City Issue Reporting";
        private String brevoBaseUrl = "https://api.brevo.com/v3";
    }
}
