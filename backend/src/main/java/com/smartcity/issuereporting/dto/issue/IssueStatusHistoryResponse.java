package com.smartcity.issuereporting.dto.issue;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssueStatusHistoryResponse {
    private UUID historyId;
    private String oldStatus;
    private String newStatus;
    private UUID changedByUserId;
    private String changedByName;
    private String note;
    private Instant changedAt;
}
