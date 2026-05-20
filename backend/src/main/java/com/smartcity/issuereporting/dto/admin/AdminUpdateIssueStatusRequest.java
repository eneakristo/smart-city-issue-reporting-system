package com.smartcity.issuereporting.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateIssueStatusRequest {

    @NotBlank(message = "status is required")
    private String status;

    @Size(max = 1000, message = "note must be at most 1000 characters")
    private String note;
}
