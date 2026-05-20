package com.smartcity.issuereporting.dto.admin;

import com.smartcity.issuereporting.model.entity.DuplicateReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDuplicateReviewDecisionRequest {

    @NotNull(message = "decision is required")
    private DuplicateReviewDecision decision;

    @Size(max = 1000, message = "reviewNotes must be at most 1000 characters")
    private String reviewNotes;
}
