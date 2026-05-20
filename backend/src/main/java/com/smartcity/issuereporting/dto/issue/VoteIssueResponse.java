package com.smartcity.issuereporting.dto.issue;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VoteIssueResponse {
    private UUID issueId;
    private long voteCount;
    private boolean currentUserHasVoted;
    private String action;
    private java.math.BigDecimal priorityScore;
}
