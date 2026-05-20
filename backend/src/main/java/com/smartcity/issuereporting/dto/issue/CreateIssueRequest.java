package com.smartcity.issuereporting.dto.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateIssueRequest {

    @NotBlank(message = "title is required")
    @Size(max = 150, message = "title must be at most 150 characters")
    private String title;

    @NotBlank(message = "description is required")
    @Size(max = 2000, message = "description must be at most 2000 characters")
    private String description;

    @NotBlank(message = "category is required")
    @Size(max = 100, message = "category must be at most 100 characters")
    private String category;

    @NotBlank(message = "severity is required")
    private String severity;

    @NotBlank(message = "locationText is required")
    @Size(max = 255, message = "locationText must be at most 255 characters")
    private String locationText;

    @DecimalMin(value = "-90.0", message = "latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "longitude must be between -180 and 180")
    private BigDecimal longitude;
}
