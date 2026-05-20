package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.config.ApplicationProperties;
import com.smartcity.issuereporting.model.entity.Category;
import com.smartcity.issuereporting.model.entity.Issue;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IssuePrioritizationServiceTest {

    private final IssuePrioritizationService prioritizationService =
            new IssuePrioritizationService(new ApplicationProperties());

    @Test
    void calculatesFullWeightedScoreForOldCentralHighImpactIssue() {
        Issue issue = buildPrimaryIssue(
                "Public Safety",
                IssueSeverityMapper.HIGH,
                new BigDecimal("41.3275"),
                new BigDecimal("19.8187"),
                Instant.parse("2026-04-25T12:00:00Z")
        );

        BigDecimal score = prioritizationService.calculatePriorityScore(
                issue,
                20,
                5,
                Instant.parse("2026-04-28T12:00:00Z")
        );

        assertEquals(new BigDecimal("100.00"), score);
    }

    @Test
    void increasesRecencyComponentWhenIssueGetsOlder() {
        Issue issue = buildPrimaryIssue(
                "Roads",
                IssueSeverityMapper.MEDIUM,
                new BigDecimal("41.3275"),
                new BigDecimal("19.8187"),
                Instant.parse("2026-04-28T00:00:00Z")
        );

        BigDecimal earlyScore = prioritizationService.calculatePriorityScore(
                issue,
                4,
                0,
                Instant.parse("2026-04-28T08:00:00Z")
        );
        BigDecimal laterScore = prioritizationService.calculatePriorityScore(
                issue,
                4,
                0,
                Instant.parse("2026-04-30T08:00:00Z")
        );

        assertEquals(new BigDecimal("58.75"), earlyScore);
        assertEquals(new BigDecimal("66.25"), laterScore);
    }

    @Test
    void returnsNullForDuplicateIssues() {
        Issue issue = buildPrimaryIssue(
                "Waste Management",
                IssueSeverityMapper.LOW,
                new BigDecimal("41.3200"),
                new BigDecimal("19.8200"),
                Instant.parse("2026-04-27T12:00:00Z")
        );
        issue.setDuplicate(true);

        assertNull(prioritizationService.calculatePriorityScore(issue, 5, 2, Instant.parse("2026-04-28T12:00:00Z")));
    }

    @Test
    void fallsBackToHalfLocationImportanceWhenCoordinatesAreMissing() {
        Issue issue = buildPrimaryIssue(
                "Roads",
                IssueSeverityMapper.HIGH,
                null,
                null,
                Instant.parse("2026-04-25T12:00:00Z")
        );

        BigDecimal score = prioritizationService.calculatePriorityScore(
                issue,
                20,
                5,
                Instant.parse("2026-04-28T12:00:00Z")
        );

        assertEquals(new BigDecimal("92.50"), score);
    }

    private Issue buildPrimaryIssue(
            String categoryName,
            int severity,
            BigDecimal latitude,
            BigDecimal longitude,
            Instant createdAt
    ) {
        Category category = new Category();
        category.setName(categoryName);

        Issue issue = new Issue();
        issue.setCategory(category);
        issue.setReportedSeverity(severity);
        issue.setLatitude(latitude);
        issue.setLongitude(longitude);
        issue.setCreatedAt(createdAt);
        issue.setDuplicate(false);
        issue.setVoteCount(0);
        issue.setDuplicateCount(0);
        return issue;
    }
}
