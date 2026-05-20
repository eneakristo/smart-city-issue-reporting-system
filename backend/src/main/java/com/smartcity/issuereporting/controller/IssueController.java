package com.smartcity.issuereporting.controller;

import com.smartcity.issuereporting.dto.issue.CreateIssueRequest;
import com.smartcity.issuereporting.dto.issue.CreateIssueResponse;
import com.smartcity.issuereporting.dto.issue.IssueDetailsResponse;
import com.smartcity.issuereporting.dto.issue.IssueQueryRequest;
import com.smartcity.issuereporting.dto.issue.IssueSummaryResponse;
import com.smartcity.issuereporting.security.AuthUser;
import com.smartcity.issuereporting.service.IssueService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping
    public List<IssueSummaryResponse> listIssues(
            @ModelAttribute IssueQueryRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return issueService.listPrimaryIssues(request, authUser);
    }

    @GetMapping("/{issueId}")
    public IssueDetailsResponse getIssueById(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return issueService.getIssueById(issueId, authUser);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<IssueSummaryResponse> listCurrentUserIssues(
            @ModelAttribute IssueQueryRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return issueService.listCurrentUserIssues(request, authUser);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreateIssueResponse createIssue(
            @Valid @RequestPart("payload") CreateIssueRequest request,
            @RequestPart("images") List<MultipartFile> images,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return issueService.createIssue(request, images, authUser);
    }
}
