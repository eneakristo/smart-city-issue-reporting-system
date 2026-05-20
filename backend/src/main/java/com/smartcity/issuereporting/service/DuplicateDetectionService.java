package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.dto.issue.DuplicateCandidateResponse;
import com.smartcity.issuereporting.model.entity.Issue;
import com.smartcity.issuereporting.model.entity.IssueStatus;
import com.smartcity.issuereporting.repository.IssueRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DuplicateDetectionService {

    private static final double DUPLICATE_CONFIDENCE_THRESHOLD = 0.70d;
    private static final double TITLE_WEIGHT = 0.30d;
    private static final double LOCATION_WEIGHT = 0.35d;
    private static final double CATEGORY_WEIGHT = 0.15d;
    private static final double DESCRIPTION_WEIGHT = 0.10d;
    private static final double TIME_PROXIMITY_WEIGHT = 0.10d;

    private final IssueRepository issueRepository;

    public DuplicateDetectionService(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public DuplicateDetectionResult detectPotentialDuplicates(Issue issue) {
        List<DuplicateCandidate> candidates = issueRepository
                .findTop20ByCategoryIdAndDuplicateFalseAndStatusInAndIdNotOrderByCreatedAtDesc(
                        issue.getCategory().getId(),
                        List.of(IssueStatus.REPORTED, IssueStatus.UNDER_REVIEW, IssueStatus.CONFIRMED, IssueStatus.IN_PROGRESS),
                        issue.getId()
                )
                .stream()
                .map(existingIssue -> toCandidate(existingIssue, issue))
                .filter(candidate -> candidate.confidence() >= DUPLICATE_CONFIDENCE_THRESHOLD)
                .sorted(Comparator.comparingDouble(DuplicateCandidate::confidence).reversed())
                .toList();

        Issue parentIssue = candidates.isEmpty() ? null : candidates.get(0).issue();
        double confidence = candidates.isEmpty() ? 0.0d : candidates.get(0).confidence();
        String matchReason = candidates.isEmpty() ? null : candidates.get(0).matchReason();

        return new DuplicateDetectionResult(
                !candidates.isEmpty(),
                parentIssue,
                confidence,
                matchReason,
                candidates.stream().map(DuplicateCandidate::toResponse).toList()
        );
    }

    private DuplicateCandidate toCandidate(Issue existingIssue, Issue incomingIssue) {
        double locationScore = similarity(normalize(incomingIssue.getLocationText()), normalize(existingIssue.getLocationText()));
        double titleScore = similarity(normalize(incomingIssue.getTitle()), normalize(existingIssue.getTitle()));
        double descriptionScore = similarity(normalize(incomingIssue.getDescription()), normalize(existingIssue.getDescription()));
        double categoryScore = categoryMatchScore(existingIssue, incomingIssue);
        double timeProximityScore = timeProximityScore(existingIssue.getCreatedAt(), incomingIssue.getCreatedAt());

        /*
         * Reports are likely duplicates when they share similar title/location text,
         * the same category, overlapping description keywords, and close report time.
         */
        double confidence = (titleScore * TITLE_WEIGHT)
                + (locationScore * LOCATION_WEIGHT)
                + (categoryScore * CATEGORY_WEIGHT)
                + (descriptionScore * DESCRIPTION_WEIGHT)
                + (timeProximityScore * TIME_PROXIMITY_WEIGHT);

        String matchReason = (
                "Title similarity %.2f, location similarity %.2f, category match %.2f, "
                        + "description keyword overlap %.2f, time proximity %.2f"
        ).formatted(titleScore, locationScore, categoryScore, descriptionScore, timeProximityScore);

        return new DuplicateCandidate(existingIssue, confidence, matchReason);
    }

    private double categoryMatchScore(Issue existingIssue, Issue incomingIssue) {
        if (existingIssue.getCategory() == null || incomingIssue.getCategory() == null) {
            return 0.0d;
        }

        return existingIssue.getCategory().getId().equals(incomingIssue.getCategory().getId()) ? 1.0d : 0.0d;
    }

    private double timeProximityScore(Instant existingCreatedAt, Instant incomingCreatedAt) {
        if (existingCreatedAt == null || incomingCreatedAt == null) {
            return 0.0d;
        }

        long daysBetween = Math.abs(Duration.between(existingCreatedAt, incomingCreatedAt).toDays());
        if (daysBetween == 0L) {
            return 1.0d;
        }
        if (daysBetween <= 3L) {
            return 0.7d;
        }
        if (daysBetween <= 7L) {
            return 0.4d;
        }

        return 0.0d;
    }

    private double similarity(String left, String right) {
        if (left.equals(right)) {
            return 1.0d;
        }

        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);

        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0.0d;
        }

        long overlap = leftTokens.stream().filter(rightTokens::contains).count();
        int denominator = Math.max(leftTokens.size(), rightTokens.size());
        return (double) overlap / denominator;
    }

    private Set<String> tokenize(String value) {
        return List.of(value.split("\\s+"))
                .stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
    }

    public record DuplicateDetectionResult(
            boolean reviewRequired,
            Issue linkedParentIssue,
            double confidence,
            String matchReason,
            List<DuplicateCandidateResponse> candidates
    ) {
    }

    private record DuplicateCandidate(Issue issue, double confidence, String matchReason) {

        private DuplicateCandidateResponse toResponse() {
            return DuplicateCandidateResponse.builder()
                    .issueId(issue.getId())
                    .title(issue.getTitle())
                    .locationText(issue.getLocationText())
                    .confidence(confidence)
                    .matchReason(matchReason)
                    .build();
        }
    }
}
