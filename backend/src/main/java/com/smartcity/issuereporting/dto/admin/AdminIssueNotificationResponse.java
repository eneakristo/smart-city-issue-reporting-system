package com.smartcity.issuereporting.dto.admin;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminIssueNotificationResponse {

    private UUID issueId;
    private String title;
    private String category;
    private String status;
    private String issueType;
    private String reporterName;
    private Instant createdAt;
}
