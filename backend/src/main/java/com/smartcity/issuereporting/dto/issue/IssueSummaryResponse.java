package com.smartcity.issuereporting.dto.issue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssueSummaryResponse {
    private UUID issueId;
    private String title;
    private String category;
    private String severity;
    private String status;
    private String issueType;
    private String locationText;
    private String zone;
    private String thumbnailUrl;
    private long voteCount;
    private boolean currentUserHasVoted;
    private BigDecimal priorityScore;
    private UUID parentIssueId;
    private String parentIssueTitle;
    private Instant createdAt;
}
