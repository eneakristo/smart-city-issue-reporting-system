package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.dto.admin.AdminDuplicateIssueSummaryResponse;
import com.smartcity.issuereporting.dto.admin.AdminDuplicateReviewDecisionRequest;
import com.smartcity.issuereporting.dto.admin.AdminDuplicateReviewResponse;
import com.smartcity.issuereporting.dto.issue.IssueImageResponse;
import com.smartcity.issuereporting.exception.BadRequestException;
import com.smartcity.issuereporting.exception.ResourceNotFoundException;
import com.smartcity.issuereporting.model.entity.DuplicateReview;
import com.smartcity.issuereporting.model.entity.DuplicateReviewDecision;
import com.smartcity.issuereporting.model.entity.Issue;
import com.smartcity.issuereporting.model.entity.IssueImage;
import com.smartcity.issuereporting.model.entity.IssueStatus;
import com.smartcity.issuereporting.model.entity.IssueStatusHistory;
import com.smartcity.issuereporting.model.entity.Role;
import com.smartcity.issuereporting.model.entity.User;
import com.smartcity.issuereporting.repository.DuplicateReviewRepository;
import com.smartcity.issuereporting.repository.IssueRepository;
import com.smartcity.issuereporting.repository.IssueStatusHistoryRepository;
import com.smartcity.issuereporting.repository.UserRepository;
import com.smartcity.issuereporting.security.AuthUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDuplicateReviewService {

    private static final BigDecimal NON_PRIORITY_DUPLICATE_SCORE = BigDecimal.ZERO;

    private final DuplicateReviewRepository duplicateReviewRepository;
    private final IssueRepository issueRepository;
    private final IssueStatusHistoryRepository issueStatusHistoryRepository;
    private final UserRepository userRepository;
    private final IssueImageStorageService issueImageStorageService;
    private final IssuePrioritizationService issuePrioritizationService;
    private final CityZoneService cityZoneService;

    public AdminDuplicateReviewService(
            DuplicateReviewRepository duplicateReviewRepository,
            IssueRepository issueRepository,
            IssueStatusHistoryRepository issueStatusHistoryRepository,
            UserRepository userRepository,
            IssueImageStorageService issueImageStorageService,
            IssuePrioritizationService issuePrioritizationService,
            CityZoneService cityZoneService
    ) {
        this.duplicateReviewRepository = duplicateReviewRepository;
        this.issueRepository = issueRepository;
        this.issueStatusHistoryRepository = issueStatusHistoryRepository;
        this.userRepository = userRepository;
        this.issueImageStorageService = issueImageStorageService;
        this.issuePrioritizationService = issuePrioritizationService;
        this.cityZoneService = cityZoneService;
    }

    @Transactional(readOnly = true)
    public List<AdminDuplicateReviewResponse> listDuplicateReviews(AuthUser authUser) {
        User administrator = getAdministrator(authUser);

        return duplicateReviewRepository.findAllByDecisionInOrderByCreatedAtDesc(List.of(
                        DuplicateReviewDecision.PENDING,
                        DuplicateReviewDecision.CONFIRMED_DUPLICATE
                ))
                .stream()
                .filter(review -> isSystemAdmin(administrator)
                        || cityZoneService.canAdministratorAccessIssue(
                                administrator.getAssignedZone(),
                                review.getDuplicateIssue()
                        ))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminDuplicateReviewResponse reviewDuplicate(
            UUID reviewId,
            AdminDuplicateReviewDecisionRequest request,
            AuthUser authUser
    ) {
        if (request.getDecision() == DuplicateReviewDecision.PENDING) {
            throw new BadRequestException("Decision must be CONFIRMED_DUPLICATE or REJECTED_DUPLICATE");
        }

        DuplicateReview review = duplicateReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Duplicate review not found: " + reviewId));
        User reviewer = getAdministrator(authUser);
        ensureCanManageIssue(reviewer, review.getDuplicateIssue());

        review.setDecision(request.getDecision());
        review.setReviewedBy(reviewer);

        if (request.getReviewNotes() != null && !request.getReviewNotes().isBlank()) {
            review.setReviewNotes(request.getReviewNotes().trim());
        }

        Issue duplicateIssue = review.getDuplicateIssue();
        Issue originalParentIssue = review.getParentIssue();
        IssueStatus oldDuplicateStatus = duplicateIssue.getStatus();

        if (request.getDecision() == DuplicateReviewDecision.CONFIRMED_DUPLICATE) {
            duplicateIssue.setDuplicate(true);
            duplicateIssue.setStatus(IssueStatus.UNDER_REVIEW);
            duplicateIssue.setPriorityScore(NON_PRIORITY_DUPLICATE_SCORE);
        } else {
            duplicateIssue.setDuplicate(false);
            duplicateIssue.setParentIssue(null);
            duplicateIssue.setDuplicateConfidence(null);
            duplicateIssue.setStatus(IssueStatus.REPORTED);
            duplicateIssue.setPriorityScore(issuePrioritizationService.calculatePriorityScore(
                    duplicateIssue,
                    duplicateIssue.getVoteCount(),
                    0,
                    Instant.now()
            ));
        }

        Issue savedDuplicateIssue = issueRepository.save(duplicateIssue);
        saveStatusHistoryIfChanged(
                savedDuplicateIssue,
                reviewer,
                oldDuplicateStatus,
                savedDuplicateIssue.getStatus(),
                "Duplicate review decision: " + request.getDecision().name()
        );
        refreshPrimaryIssuePriority(originalParentIssue);
        return toResponse(duplicateReviewRepository.save(review));
    }

    private AdminDuplicateReviewResponse toResponse(DuplicateReview review) {
        Issue duplicateIssue = review.getDuplicateIssue();

        return AdminDuplicateReviewResponse.builder()
                .reviewId(review.getId())
                .duplicateIssueId(duplicateIssue.getId())
                .reportedTitle(duplicateIssue.getTitle())
                .category(duplicateIssue.getCategory().getName())
                .locationText(duplicateIssue.getLocationText())
                .status(duplicateIssue.getStatus().name())
                .issueType(toIssueTypeLabel(duplicateIssue))
                .images(duplicateIssue.getImages().stream()
                        .sorted(Comparator.comparing(IssueImage::getDisplayOrder))
                        .map(this::toImageResponse)
                        .toList())
                .linkedOriginalIssue(toLinkedOriginalIssue(review.getParentIssue()))
                .duplicateConfidence(review.getDuplicateConfidence())
                .matchReason(review.getMatchReason())
                .reviewDecision(review.getDecision().name())
                .reviewNotes(review.getReviewNotes())
                .reviewedByUserId(review.getReviewedBy() == null ? null : review.getReviewedBy().getId())
                .reviewedByName(review.getReviewedBy() == null ? null : review.getReviewedBy().getFullName())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private AdminDuplicateIssueSummaryResponse toLinkedOriginalIssue(Issue issue) {
        if (issue == null) {
            return null;
        }

        return AdminDuplicateIssueSummaryResponse.builder()
                .issueId(issue.getId())
                .title(issue.getTitle())
                .category(issue.getCategory().getName())
                .status(issue.getStatus().name())
                .issueType(toIssueTypeLabel(issue))
                .locationText(issue.getLocationText())
                .build();
    }

    private String toIssueTypeLabel(Issue issue) {
        return issue.isDuplicate() ? "DUPLICATE" : "PRIMARY";
    }

    private IssueImageResponse toImageResponse(IssueImage image) {
        return IssueImageResponse.builder()
                .id(image.getId())
                .publicUrl(issueImageStorageService.resolveAccessUrl(image.getStoragePath(), image.getPublicUrl()))
                .originalFileName(image.getOriginalFileName())
                .contentType(image.getContentType())
                .fileSizeBytes(image.getFileSizeBytes())
                .displayOrder(image.getDisplayOrder())
                .build();
    }

    private void refreshPrimaryIssuePriority(Issue issue) {
        if (issue == null || issue.isDuplicate()) {
            return;
        }

        long activeDuplicateCount = issueRepository.countByParentIssueId(issue.getId());
        BigDecimal priorityScore = issuePrioritizationService.calculatePriorityScore(
                issue,
                issue.getVoteCount(),
                Math.toIntExact(activeDuplicateCount),
                Instant.now()
        );

        issue.setDuplicateCount(Math.toIntExact(activeDuplicateCount));
        issue.setPriorityScore(priorityScore);
        issueRepository.save(issue);
    }

    private void saveStatusHistoryIfChanged(
            Issue issue,
            User reviewer,
            IssueStatus oldStatus,
            IssueStatus newStatus,
            String note
    ) {
        if (oldStatus == newStatus) {
            return;
        }

        IssueStatusHistory statusHistory = new IssueStatusHistory();
        statusHistory.setIssue(issue);
        statusHistory.setChangedBy(reviewer);
        statusHistory.setOldStatus(oldStatus);
        statusHistory.setNewStatus(newStatus);
        statusHistory.setNote(note);
        issueStatusHistoryRepository.save(statusHistory);
    }

    private User getAdministrator(AuthUser authUser) {
        return userRepository.findById(authUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated administrator not found"));
    }

    private void ensureCanManageIssue(User administrator, Issue issue) {
        if (isSystemAdmin(administrator)) {
            return;
        }

        if (!cityZoneService.canAdministratorAccessIssue(administrator.getAssignedZone(), issue)) {
            throw new ResourceNotFoundException("Duplicate review not found");
        }
    }

    private boolean isSystemAdmin(User user) {
        return user.getRole() == Role.SYSTEM_ADMIN;
    }
}
