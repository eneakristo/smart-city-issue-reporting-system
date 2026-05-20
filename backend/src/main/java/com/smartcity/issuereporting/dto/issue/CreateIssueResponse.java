package com.smartcity.issuereporting.dto.issue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateIssueResponse {
    private UUID issueId;
    private String title;
    private String category;
    private String severity;
    private String status;
    private String issueType;
    private String locationText;
    private String zone;
    private UUID reporterId;
    private boolean duplicateReviewRequired;
    private UUID linkedParentIssueId;
    private List<DuplicateCandidateResponse> duplicateCandidates;
    private List<IssueImageResponse> images;
    private Instant createdAt;
}
