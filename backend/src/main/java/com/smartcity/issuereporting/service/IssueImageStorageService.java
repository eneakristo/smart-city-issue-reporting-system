package com.smartcity.issuereporting.service;

import org.springframework.web.multipart.MultipartFile;

public interface IssueImageStorageService {

    StoredIssueImage store(String issueStoragePrefix, MultipartFile file);

    String resolveAccessUrl(String storagePath, String fallbackUrl);

    record StoredIssueImage(
            String storagePath,
            String publicUrl,
            String originalFileName,
            String contentType,
            long fileSizeBytes
    ) {
    }
}
