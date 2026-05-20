package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.config.ApplicationProperties;
import com.smartcity.issuereporting.model.entity.Issue;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class IssuePrioritizationService {

    private static final MathContext MATH_CONTEXT = MathContext.DECIMAL64;

    private static final BigDecimal SEVERITY_WEIGHT = BigDecimal.valueOf(35);
    private static final BigDecimal VOTE_WEIGHT = BigDecimal.valueOf(25);
    private static final BigDecimal LOCATION_WEIGHT = BigDecimal.valueOf(15);
    private static final BigDecimal RECENCY_WEIGHT = BigDecimal.valueOf(15);
    private static final BigDecimal CATEGORY_WEIGHT = BigDecimal.valueOf(5);
    private static final BigDecimal DUPLICATE_WEIGHT = BigDecimal.valueOf(5);

    private static final BigDecimal FULL_IMPORTANCE = BigDecimal.ONE;
    private static final BigDecimal MEDIUM_HIGH_IMPORTANCE = BigDecimal.valueOf(0.75d);
    private static final BigDecimal HALF_IMPORTANCE = BigDecimal.valueOf(0.5d);

    private static final BigDecimal LOW_SEVERITY_IMPORTANCE = HALF_IMPORTANCE;
    private static final BigDecimal MEDIUM_SEVERITY_IMPORTANCE = MEDIUM_HIGH_IMPORTANCE;
    private static final BigDecimal HIGH_SEVERITY_IMPORTANCE = FULL_IMPORTANCE;

    private static final Map<String, BigDecimal> CATEGORY_IMPORTANCE_BY_NAME = Map.ofEntries(
            Map.entry("PUBLIC SAFETY", FULL_IMPORTANCE),
            Map.entry("WATER SUPPLY", FULL_IMPORTANCE),
            Map.entry("ROADS", FULL_IMPORTANCE),
            Map.entry("STREET LIGHTING", MEDIUM_HIGH_IMPORTANCE),
            Map.entry("WASTE MANAGEMENT", MEDIUM_HIGH_IMPORTANCE),
            Map.entry("PARKS AND GREEN AREAS", HALF_IMPORTANCE)
    );

    private final ApplicationProperties applicationProperties;

    public IssuePrioritizationService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public BigDecimal calculatePriorityScore(Issue issue, long activeVoteCount) {
        return calculatePriorityScore(issue, activeVoteCount, issue.getDuplicateCount(), Instant.now());
    }

    public BigDecimal calculatePriorityScore(Issue issue, long activeVoteCount, int activeDuplicateCount, Instant evaluationTime) {
        if (issue.isDuplicate()) {
            return null;
        }

        PriorityBreakdown breakdown = calculateBreakdown(issue, activeVoteCount, activeDuplicateCount, evaluationTime);
        return breakdown.totalScore().setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateBaselineVotesOnlyScore(long activeVoteCount) {
        return normalizeCount(activeVoteCount, applicationProperties.getPrioritization().getVoteCountForFullScore())
                .multiply(BigDecimal.valueOf(100), MATH_CONTEXT)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public PriorityBreakdown calculateBreakdown(Issue issue, long activeVoteCount, int activeDuplicateCount, Instant evaluationTime) {
        Instant effectiveEvaluationTime = evaluationTime == null ? Instant.now() : evaluationTime;

        // Priority is a weighted 0-100 score. Each factor is normalized to 0..1,
        // multiplied by its weight, then added together. Duplicates do not get
        // ranked directly, but confirmed/linked duplicate reports strengthen the
        // original issue through the duplicate component.
        BigDecimal severityComponent = weighted(severityImportance(issue), SEVERITY_WEIGHT);
        BigDecimal voteComponent = weighted(
                normalizeCount(activeVoteCount, applicationProperties.getPrioritization().getVoteCountForFullScore()),
                VOTE_WEIGHT
        );
        BigDecimal locationComponent = weighted(locationImportance(issue), LOCATION_WEIGHT);
        BigDecimal recencyComponent = weighted(recencyImportance(issue, effectiveEvaluationTime), RECENCY_WEIGHT);
        BigDecimal categoryComponent = weighted(categoryImportance(issue), CATEGORY_WEIGHT);
        BigDecimal duplicateComponent = weighted(
                normalizeCount(activeDuplicateCount, applicationProperties.getPrioritization().getDuplicateCountForFullScore()),
                DUPLICATE_WEIGHT
        );

        BigDecimal totalScore = severityComponent
                .add(voteComponent)
                .add(locationComponent)
                .add(recencyComponent)
                .add(categoryComponent)
                .add(duplicateComponent);

        return new PriorityBreakdown(
                severityComponent,
                voteComponent,
                locationComponent,
                recencyComponent,
                categoryComponent,
                duplicateComponent,
                totalScore
        );
    }

    private BigDecimal weighted(BigDecimal factorScore, BigDecimal weight) {
        return factorScore.multiply(weight, MATH_CONTEXT);
    }

    private BigDecimal normalizeCount(long value, int fullScoreThreshold) {
        if (value <= 0 || fullScoreThreshold <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal normalized = BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(fullScoreThreshold), 4, RoundingMode.HALF_UP);

        return normalized.min(BigDecimal.ONE);
    }

    private BigDecimal severityImportance(Issue issue) {
        Integer severity = issue.getVerifiedSeverity() != null ? issue.getVerifiedSeverity() : issue.getReportedSeverity();

        return switch (severity) {
            case IssueSeverityMapper.LOW -> LOW_SEVERITY_IMPORTANCE;
            case IssueSeverityMapper.MEDIUM -> MEDIUM_SEVERITY_IMPORTANCE;
            case IssueSeverityMapper.HIGH -> HIGH_SEVERITY_IMPORTANCE;
            default -> MEDIUM_SEVERITY_IMPORTANCE;
        };
    }

    private BigDecimal locationImportance(Issue issue) {
        if (issue.getLatitude() == null || issue.getLongitude() == null) {
            return HALF_IMPORTANCE;
        }

        double centerLatitude = applicationProperties.getPrioritization().getCityCenterLatitude().doubleValue();
        double centerLongitude = applicationProperties.getPrioritization().getCityCenterLongitude().doubleValue();
        double distanceKm = haversineDistanceKm(
                issue.getLatitude().doubleValue(),
                issue.getLongitude().doubleValue(),
                centerLatitude,
                centerLongitude
        );

        if (distanceKm <= 1.0d) {
            return FULL_IMPORTANCE;
        }
        if (distanceKm <= 3.0d) {
            return MEDIUM_HIGH_IMPORTANCE;
        }
        return HALF_IMPORTANCE;
    }

    private BigDecimal recencyImportance(Issue issue, Instant evaluationTime) {
        if (issue.getCreatedAt() == null) {
            return HALF_IMPORTANCE;
        }

        Duration age = Duration.between(issue.getCreatedAt(), evaluationTime);
        long ageHours = Math.max(0L, age.toHours());

        if (ageHours >= 48L) {
            return FULL_IMPORTANCE;
        }
        if (ageHours >= 24L) {
            return MEDIUM_HIGH_IMPORTANCE;
        }
        return HALF_IMPORTANCE;
    }

    private BigDecimal categoryImportance(Issue issue) {
        String normalizedCategory = issue.getCategory() == null || issue.getCategory().getName() == null
                ? ""
                : issue.getCategory().getName().trim().toUpperCase(Locale.ROOT);

        return CATEGORY_IMPORTANCE_BY_NAME.getOrDefault(normalizedCategory, MEDIUM_HIGH_IMPORTANCE);
    }

    private double haversineDistanceKm(double latitude1, double longitude1, double latitude2, double longitude2) {
        double earthRadiusKm = 6371.0d;
        double latitudeDelta = Math.toRadians(latitude2 - latitude1);
        double longitudeDelta = Math.toRadians(longitude2 - longitude1);
        double latitude1Radians = Math.toRadians(latitude1);
        double latitude2Radians = Math.toRadians(latitude2);

        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(latitude1Radians) * Math.cos(latitude2Radians) * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    public record PriorityBreakdown(
            BigDecimal severityComponent,
            BigDecimal voteComponent,
            BigDecimal locationComponent,
            BigDecimal recencyComponent,
            BigDecimal categoryComponent,
            BigDecimal duplicateComponent,
            BigDecimal totalScore
    ) {
    }
}
