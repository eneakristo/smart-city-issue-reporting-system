package com.smartcity.issuereporting.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateIssueSeverityRequest {

    @NotBlank(message = "severity is required")
    private String severity;
}
