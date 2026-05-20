package com.smartcity.issuereporting.dto.issue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssueDetailsResponse {
    private UUID issueId;
    private String title;
    private String description;
    private String category;
    private String severity;
    private String status;
    private String issueType;
    private String locationText;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String zone;
    private BigDecimal priorityScore;
    private long voteCount;
    private boolean currentUserHasVoted;
    private UUID reporterId;
    private String reporterName;
    private UUID parentIssueId;
    private long linkedDuplicateCount;
    private List<IssueImageResponse> images;
    private List<IssueImageResponse> resolutionImages;
    private List<IssueStatusHistoryResponse> statusHistory;
    private Instant createdAt;
    private Instant updatedAt;
}
