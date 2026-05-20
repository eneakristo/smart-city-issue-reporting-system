package com.smartcity.issuereporting.dto.admin;

import com.smartcity.issuereporting.dto.issue.IssueImageResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDuplicateReviewResponse {
    private UUID reviewId;
    private UUID duplicateIssueId;
    private String reportedTitle;
    private String category;
    private String locationText;
    private String status;
    private String issueType;
    private List<IssueImageResponse> images;
    private AdminDuplicateIssueSummaryResponse linkedOriginalIssue;
    private BigDecimal duplicateConfidence;
    private String matchReason;
    private String reviewDecision;
    private String reviewNotes;
    private UUID reviewedByUserId;
    private String reviewedByName;
    private Instant createdAt;
    private Instant updatedAt;
}
