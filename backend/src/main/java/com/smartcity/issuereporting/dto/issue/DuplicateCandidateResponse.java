package com.smartcity.issuereporting.dto.issue;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DuplicateCandidateResponse {
    private UUID issueId;
    private String title;
    private String locationText;
    private double confidence;
    private String matchReason;
}
