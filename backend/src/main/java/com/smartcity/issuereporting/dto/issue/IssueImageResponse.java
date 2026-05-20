package com.smartcity.issuereporting.dto.issue;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IssueImageResponse {
    private UUID id;
    private String publicUrl;
    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;
    private Integer displayOrder;
}
