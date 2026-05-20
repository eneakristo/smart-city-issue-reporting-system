package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.exception.BadRequestException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class IssueSeverityMapper {

    public static final int LOW = 1;
    public static final int MEDIUM = 2;
    public static final int HIGH = 3;

    public Integer toDatabaseValue(String severity) {
        if (severity == null || severity.isBlank()) {
            throw new BadRequestException("Severity must be one of LOW, MEDIUM, HIGH");
        }

        return switch (severity.trim().toUpperCase(Locale.ROOT)) {
            case "LOW" -> LOW;
            case "MEDIUM" -> MEDIUM;
            case "HIGH" -> HIGH;
            default -> throw new BadRequestException("Severity must be one of LOW, MEDIUM, HIGH");
        };
    }

    public String toApiValue(Integer severity) {
        if (severity == null) {
            return null;
        }

        return switch (severity) {
            case LOW -> "LOW";
            case MEDIUM -> "MEDIUM";
            case HIGH -> "HIGH";
            default -> "UNKNOWN";
        };
    }
}
