package com.smartcity.issuereporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartcity.issuereporting.model.entity.Category;
import com.smartcity.issuereporting.model.entity.Issue;
import com.smartcity.issuereporting.model.entity.IssueStatus;
import com.smartcity.issuereporting.repository.IssueRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

    private static final Instant REPORT_TIME = Instant.parse("2026-04-30T10:00:00Z");

    @Mock
    private IssueRepository issueRepository;

    @Test
    void detectsLikelyDuplicateAgainstPrimaryIssuesOnly() {
        Category category = category();
        Issue incomingIssue = issue(
                category,
                "Large pothole near city hall",
                "A deep pothole is blocking the right lane by the city hall entrance",
                "City Hall main entrance"
        );
        Issue existingIssue = issue(
                category,
                "Large pothole near city hall",
                "Deep pothole blocking the right lane near the city hall entrance",
                "City Hall main entrance"
        );

        when(issueRepository.findTop20ByCategoryIdAndDuplicateFalseAndStatusInAndIdNotOrderByCreatedAtDesc(
                eq(category.getId()),
                any(),
                eq(incomingIssue.getId())
        )).thenReturn(List.of(existingIssue));

        DuplicateDetectionService service = new DuplicateDetectionService(issueRepository);

        DuplicateDetectionService.DuplicateDetectionResult result = service.detectPotentialDuplicates(incomingIssue);

        assertThat(result.reviewRequired()).isTrue();
        assertThat(result.linkedParentIssue()).isEqualTo(existingIssue);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(0.70d);
        assertThat(result.matchReason()).contains(
                "Title similarity",
                "location similarity",
                "category match",
                "description keyword overlap",
                "time proximity"
        );
        assertThat(result.candidates()).hasSize(1);

        verify(issueRepository).findTop20ByCategoryIdAndDuplicateFalseAndStatusInAndIdNotOrderByCreatedAtDesc(
                eq(category.getId()),
                any(),
                eq(incomingIssue.getId())
        );
    }

    @Test
    void ignoresCandidateBelowDuplicateThreshold() {
        Category category = category();
        Issue incomingIssue = issue(
                category,
                "Broken traffic light",
                "The signal is not working at the north intersection",
                "North boulevard"
        );
        Issue unrelatedIssue = issue(
                category,
                "Overflowing trash bin",
                "Garbage is collecting next to the park bench",
                "Central park"
        );

        when(issueRepository.findTop20ByCategoryIdAndDuplicateFalseAndStatusInAndIdNotOrderByCreatedAtDesc(
                eq(category.getId()),
                any(),
                eq(incomingIssue.getId())
        )).thenReturn(List.of(unrelatedIssue));

        DuplicateDetectionService service = new DuplicateDetectionService(issueRepository);

        DuplicateDetectionService.DuplicateDetectionResult result = service.detectPotentialDuplicates(incomingIssue);

        assertThat(result.reviewRequired()).isFalse();
        assertThat(result.linkedParentIssue()).isNull();
        assertThat(result.confidence()).isZero();
        assertThat(result.candidates()).isEmpty();
    }

    private Category category() {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Roads");
        return category;
    }

    private Issue issue(Category category, String title, String description, String locationText) {
        Issue issue = new Issue();
        issue.setId(UUID.randomUUID());
        issue.setCategory(category);
        issue.setTitle(title);
        issue.setDescription(description);
        issue.setLocationText(locationText);
        issue.setCreatedAt(REPORT_TIME);
        issue.setUpdatedAt(REPORT_TIME);
        return issue;
    }
}
