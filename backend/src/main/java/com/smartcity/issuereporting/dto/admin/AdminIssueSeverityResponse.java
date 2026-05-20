package com.smartcity.issuereporting.dto.admin;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminIssueSeverityResponse {
    private UUID issueId;
    private String title;
    private String reportedSeverity;
    private String verifiedSeverity;
    private String effectiveSeverity;
    private BigDecimal priorityScore;
}
