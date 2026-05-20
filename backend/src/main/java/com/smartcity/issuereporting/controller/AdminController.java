package com.smartcity.issuereporting.controller;

import com.smartcity.issuereporting.dto.admin.AdminDuplicateReviewDecisionRequest;
import com.smartcity.issuereporting.dto.admin.AdminDuplicateReviewResponse;
import com.smartcity.issuereporting.dto.admin.AdminIssueNotificationResponse;
import com.smartcity.issuereporting.dto.admin.AdminIssueSeverityResponse;
import com.smartcity.issuereporting.dto.admin.AdminUpdateIssueStatusRequest;
import com.smartcity.issuereporting.dto.admin.AdminUpdateIssueSeverityRequest;
import com.smartcity.issuereporting.dto.issue.IssueQueryRequest;
import com.smartcity.issuereporting.dto.issue.IssueSummaryResponse;
import com.smartcity.issuereporting.security.AuthUser;
import com.smartcity.issuereporting.service.AdminDuplicateReviewService;
import com.smartcity.issuereporting.service.AdminIssueService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/administrator")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ADMIN', 'SYSTEM_ADMIN')")
public class AdminController {

    private final AdminDuplicateReviewService adminDuplicateReviewService;
    private final AdminIssueService adminIssueService;

    public AdminController(
            AdminDuplicateReviewService adminDuplicateReviewService,
            AdminIssueService adminIssueService
    ) {
        this.adminDuplicateReviewService = adminDuplicateReviewService;
        this.adminIssueService = adminIssueService;
    }

    @GetMapping("/dashboard")
    public Map<String, String> dashboard() {
        return Map.of("message", "Administrator dashboard placeholder");
    }

    @GetMapping("/duplicates")
    public List<AdminDuplicateReviewResponse> listDuplicateReviews(@AuthenticationPrincipal AuthUser authUser) {
        return adminDuplicateReviewService.listDuplicateReviews(authUser);
    }

    @GetMapping("/issues")
    public List<IssueSummaryResponse> listPrimaryIssues(
            @ModelAttribute IssueQueryRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return adminIssueService.listPrimaryIssues(request, authUser);
    }

    @GetMapping("/notifications/issues")
    public List<AdminIssueNotificationResponse> listIssueNotifications(@AuthenticationPrincipal AuthUser authUser) {
        return adminIssueService.listRecentIssueNotifications(authUser);
    }

    @PatchMapping("/notifications/issues/{issueId}/seen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markIssueNotificationSeen(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        adminIssueService.markNotificationSeen(issueId, authUser);
    }

    @PatchMapping("/duplicates/{reviewId}")
    public AdminDuplicateReviewResponse reviewDuplicate(
            @PathVariable UUID reviewId,
            @Valid @RequestBody AdminDuplicateReviewDecisionRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return adminDuplicateReviewService.reviewDuplicate(reviewId, request, authUser);
    }

    @PatchMapping("/issues/{issueId}/severity")
    public AdminIssueSeverityResponse updateIssueSeverity(
            @PathVariable UUID issueId,
            @Valid @RequestBody AdminUpdateIssueSeverityRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return adminIssueService.updateVerifiedSeverity(issueId, request, authUser);
    }

    @DeleteMapping("/issues/{issueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssue(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        adminIssueService.deleteIssue(issueId, authUser);
    }

    @PatchMapping(value = "/issues/{issueId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public IssueSummaryResponse updateIssueStatus(
            @PathVariable UUID issueId,
            @Valid @RequestBody AdminUpdateIssueStatusRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return adminIssueService.updateStatus(issueId, request, null, authUser);
    }

    @PatchMapping(value = "/issues/{issueId}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IssueSummaryResponse updateIssueStatusWithResolutionImage(
            @PathVariable UUID issueId,
            @RequestPart("status") String status,
            @RequestPart(value = "note", required = false) String note,
            @RequestPart(value = "resolutionImage", required = false) MultipartFile resolutionImage,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        AdminUpdateIssueStatusRequest request = new AdminUpdateIssueStatusRequest();
        request.setStatus(status);
        request.setNote(note);
        return adminIssueService.updateStatus(issueId, request, resolutionImage, authUser);
    }
}
