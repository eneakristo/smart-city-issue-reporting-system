package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.dto.admin.AdminIssueNotificationResponse;
import com.smartcity.issuereporting.dto.admin.AdminIssueSeverityResponse;
import com.smartcity.issuereporting.dto.admin.AdminUpdateIssueStatusRequest;
import com.smartcity.issuereporting.dto.admin.AdminUpdateIssueSeverityRequest;
import com.smartcity.issuereporting.dto.issue.IssueListSort;
import com.smartcity.issuereporting.dto.issue.IssueQueryRequest;
import com.smartcity.issuereporting.dto.issue.IssueSummaryResponse;
import com.smartcity.issuereporting.exception.BadRequestException;
import com.smartcity.issuereporting.exception.ResourceNotFoundException;
import com.smartcity.issuereporting.model.entity.Issue;
import com.smartcity.issuereporting.model.entity.IssueImage;
import com.smartcity.issuereporting.model.entity.IssueStatus;
import com.smartcity.issuereporting.model.entity.IssueStatusHistory;
import com.smartcity.issuereporting.model.entity.Role;
import com.smartcity.issuereporting.model.entity.User;
import com.smartcity.issuereporting.repository.IssueRepository;
import com.smartcity.issuereporting.repository.IssueSpecifications;
import com.smartcity.issuereporting.repository.IssueStatusHistoryRepository;
import com.smartcity.issuereporting.repository.DuplicateReviewRepository;
import com.smartcity.issuereporting.repository.IssueImageRepository;
import com.smartcity.issuereporting.repository.UserRepository;
import com.smartcity.issuereporting.repository.VoteRepository;
import com.smartcity.issuereporting.security.AuthUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminIssueService {

    private final IssueRepository issueRepository;
    private final IssueStatusHistoryRepository issueStatusHistoryRepository;
    private final DuplicateReviewRepository duplicateReviewRepository;
    private final IssueImageRepository issueImageRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final IssueSeverityMapper issueSeverityMapper;
    private final IssuePrioritizationService issuePrioritizationService;
    private final IssueImageStorageService issueImageStorageService;
    private final CityZoneService cityZoneService;

    public AdminIssueService(
            IssueRepository issueRepository,
            IssueStatusHistoryRepository issueStatusHistoryRepository,
            DuplicateReviewRepository duplicateReviewRepository,
            IssueImageRepository issueImageRepository,
            VoteRepository voteRepository,
            UserRepository userRepository,
            IssueSeverityMapper issueSeverityMapper,
            IssuePrioritizationService issuePrioritizationService,
            IssueImageStorageService issueImageStorageService,
            CityZoneService cityZoneService
    ) {
        this.issueRepository = issueRepository;
        this.issueStatusHistoryRepository = issueStatusHistoryRepository;
        this.duplicateReviewRepository = duplicateReviewRepository;
        this.issueImageRepository = issueImageRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.issueSeverityMapper = issueSeverityMapper;
        this.issuePrioritizationService = issuePrioritizationService;
        this.issueImageStorageService = issueImageStorageService;
        this.cityZoneService = cityZoneService;
    }

    @Transactional(readOnly = true)
    public List<IssueSummaryResponse> listPrimaryIssues(IssueQueryRequest request, AuthUser authUser) {
        User administrator = getAdministrator(authUser);
        List<Issue> issues = isSystemAdmin(administrator)
                ? issueRepository.findAll(IssueSpecifications.publicPrimaryIssues(request))
                : issueRepository.findAll(IssueSpecifications.publicPrimaryIssues(request)).stream()
                        .filter(issue -> cityZoneService.canAdministratorAccessIssue(
                                administrator.getAssignedZone(),
                                issue
                        ))
                        .toList();

        return sortIssues(issues, request.getParsedSort()).stream()
                .map(this::toIssueSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminIssueNotificationResponse> listRecentIssueNotifications(AuthUser authUser) {
        User administrator = getAdministrator(authUser);

        return issueRepository.findTop50ByStatusInOrderByCreatedAtDesc(List.of(
                        IssueStatus.REPORTED,
                        IssueStatus.UNDER_REVIEW
                ))
                .stream()
                .filter(issue -> isSystemAdmin(administrator)
                        || cityZoneService.canAdministratorAccessIssue(administrator.getAssignedZone(), issue))
                .limit(12)
                .map(this::toNotificationResponse)
                .toList();
    }

    @Transactional
    public void markNotificationSeen(UUID issueId, AuthUser authUser) {
        Issue issue = issueRepository.findWithDetailsById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

        User administrator = getAdministrator(authUser);
        ensureCanManageIssue(administrator, issue);

        if (issue.getAdministratorSeenAt() != null) {
            return;
        }

        issue.setAdministratorSeenAt(Instant.now());
        issue.setAdministratorSeenBy(administrator);
        issueRepository.save(issue);
    }

    @Transactional
    public AdminIssueSeverityResponse updateVerifiedSeverity(
            UUID issueId,
            AdminUpdateIssueSeverityRequest request,
            AuthUser authUser
    ) {
        Issue issue = issueRepository.findWithDetailsById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
        ensureCanManageIssue(getAdministrator(authUser), issue);

        if (issue.isDuplicate()) {
            throw new BadRequestException("Severity can only be verified on original issues");
        }

        issue.setVerifiedSeverity(issueSeverityMapper.toDatabaseValue(request.getSeverity()));
        issue.setDuplicateCount(Math.toIntExact(issueRepository.countByParentIssueId(issue.getId())));
        issue.setPriorityScore(calculateCurrentPriorityScore(issue));

        return toResponse(issueRepository.save(issue));
    }

    @Transactional
    public void deleteIssue(UUID issueId, AuthUser authUser) {
        Issue issue = issueRepository.findWithDetailsById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
        ensureCanManageIssue(getAdministrator(authUser), issue);

        if (!issue.isDuplicate()) {
            issueRepository.findByParentIssueId(issue.getId()).forEach(this::deleteSingleIssue);
        }

        deleteSingleIssue(issue);
    }

    @Transactional
    public IssueSummaryResponse updateStatus(
            UUID issueId,
            AdminUpdateIssueStatusRequest request,
            MultipartFile resolutionImage,
            AuthUser authUser
    ) {
        Issue issue = issueRepository.findWithDetailsById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
        User admin = getAdministrator(authUser);
        ensureCanManageIssue(admin, issue);

        if (issue.isDuplicate()) {
            throw new BadRequestException("Status can only be updated on original issues");
        }

        IssueStatus oldStatus = issue.getStatus();
        IssueStatus newStatus = toIssueStatus(request.getStatus());
        if (oldStatus == newStatus) {
            return toIssueSummaryResponse(issue);
        }

        if (newStatus == IssueStatus.RESOLVED && isMissingFile(resolutionImage)
                && issueImageRepository.findByIssueIdAndResolutionProofTrueOrderByDisplayOrderAsc(issue.getId()).isEmpty()) {
            throw new BadRequestException("A resolution photo is required before marking this issue as resolved");
        }

        issue.setStatus(newStatus);

        Issue savedIssue = issueRepository.save(issue);
        if (newStatus == IssueStatus.RESOLVED && !isMissingFile(resolutionImage)) {
            saveResolutionImage(savedIssue, resolutionImage);
        }
        saveStatusHistory(savedIssue, admin, oldStatus, newStatus, request.getNote());

        return toIssueSummaryResponse(savedIssue);
    }

    private void saveResolutionImage(Issue issue, MultipartFile image) {
        IssueImageStorageService.StoredIssueImage storedImage =
                issueImageStorageService.store(
                        issueImageStoragePrefix(issue.getId().toString()) + "/resolution",
                        image
                );

        int nextDisplayOrder = issueImageRepository.findByIssueIdAndResolutionProofTrueOrderByDisplayOrderAsc(issue.getId())
                .stream()
                .map(IssueImage::getDisplayOrder)
                .max(Integer::compareTo)
                .orElse(-1) + 1;

        IssueImage issueImage = new IssueImage();
        issueImage.setIssue(issue);
        issueImage.setStoragePath(storedImage.storagePath());
        issueImage.setPublicUrl(storedImage.publicUrl());
        issueImage.setOriginalFileName(storedImage.originalFileName());
        issueImage.setContentType(storedImage.contentType());
        issueImage.setFileSizeBytes(storedImage.fileSizeBytes());
        issueImage.setDisplayOrder(nextDisplayOrder);
        issueImage.setResolutionProof(true);
        issueImageRepository.save(issueImage);
    }

    private boolean isMissingFile(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private String issueImageStoragePrefix(String issueId) {
        return "issues/" + issueId;
    }

    private BigDecimal calculateCurrentPriorityScore(Issue issue) {
        return issuePrioritizationService.calculatePriorityScore(
                issue,
                issue.getVoteCount(),
                issue.getDuplicateCount(),
                Instant.now()
        );
    }

    private List<Issue> sortIssues(List<Issue> issues, IssueListSort sort) {
        Comparator<Issue> comparator = Comparator.comparing(Issue::getCreatedAt, Comparator.reverseOrder());
        if (sort == IssueListSort.MOST_VOTED) {
            comparator = Comparator
                    .comparingInt(Issue::getVoteCount).reversed()
                    .thenComparing(Issue::getCreatedAt, Comparator.reverseOrder());
        } else if (sort == IssueListSort.HIGHEST_PRIORITY) {
            comparator = Comparator
                    .comparing(this::currentPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Issue::getCreatedAt, Comparator.reverseOrder());
        }

        return issues.stream().sorted(comparator).toList();
    }

    private IssueStatus toIssueStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status must be one of OPEN, REPORTED, CONFIRMED, IN_PROGRESS, RESOLVED, REJECTED");
        }

        String normalizedStatus = status.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        if ("OPEN".equals(normalizedStatus)) {
            return IssueStatus.REPORTED;
        }

        try {
            IssueStatus issueStatus = IssueStatus.valueOf(normalizedStatus);
            if (issueStatus == IssueStatus.UNDER_REVIEW) {
                throw new BadRequestException("Status must be one of OPEN, REPORTED, CONFIRMED, IN_PROGRESS, RESOLVED, REJECTED");
            }
            return issueStatus;
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Status must be one of OPEN, REPORTED, CONFIRMED, IN_PROGRESS, RESOLVED, REJECTED");
        }
    }

    private void saveStatusHistory(
            Issue issue,
            User admin,
            IssueStatus oldStatus,
            IssueStatus newStatus,
            String note
    ) {
        IssueStatusHistory statusHistory = new IssueStatusHistory();
        statusHistory.setIssue(issue);
        statusHistory.setChangedBy(admin);
        statusHistory.setOldStatus(oldStatus);
        statusHistory.setNewStatus(newStatus);
        statusHistory.setNote(note == null || note.isBlank() ? "Administrator status update" : note.trim());
        issueStatusHistoryRepository.save(statusHistory);
    }

    private void deleteSingleIssue(Issue issue) {
        duplicateReviewRepository.deleteByDuplicateIssueId(issue.getId());
        voteRepository.deleteByIssueId(issue.getId());
        issueImageRepository.deleteByIssueId(issue.getId());
        issueStatusHistoryRepository.deleteByIssueId(issue.getId());
        issueRepository.delete(issue);
    }

    private IssueSummaryResponse toIssueSummaryResponse(Issue issue) {
        return IssueSummaryResponse.builder()
                .issueId(issue.getId())
                .title(issue.getTitle())
                .category(issue.getCategory().getName())
                .severity(issueSeverityMapper.toApiValue(effectiveSeverity(issue)))
                .status(issue.getStatus().name())
                .issueType("PRIMARY")
                .locationText(issue.getLocationText())
                .zone(toZoneLabel(issue))
                .thumbnailUrl(resolveThumbnailUrl(issue))
                .voteCount(issue.getVoteCount())
                .currentUserHasVoted(false)
                .priorityScore(currentPriorityScore(issue))
                .parentIssueId(null)
                .parentIssueTitle(null)
                .createdAt(issue.getCreatedAt())
                .build();
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
            throw new ResourceNotFoundException("Issue not found: " + issue.getId());
        }
    }

    private boolean isSystemAdmin(User user) {
        return user.getRole() == Role.SYSTEM_ADMIN;
    }

    private String toZoneLabel(Issue issue) {
        return cityZoneService.resolveZone(issue) == null ? null : cityZoneService.resolveZone(issue).name();
    }

    private AdminIssueNotificationResponse toNotificationResponse(Issue issue) {
        return AdminIssueNotificationResponse.builder()
                .issueId(issue.getId())
                .title(issue.getTitle())
                .category(issue.getCategory().getName())
                .status(issue.getStatus().name())
                .issueType(issue.isDuplicate() ? "DUPLICATE" : "PRIMARY")
                .reporterName(issue.getCreatedByUser().getFullName())
                .createdAt(issue.getCreatedAt())
                .build();
    }

    private Integer effectiveSeverity(Issue issue) {
        return issue.getVerifiedSeverity() != null ? issue.getVerifiedSeverity() : issue.getReportedSeverity();
    }

    private BigDecimal currentPriorityScore(Issue issue) {
        if (issue.getPriorityScore() != null) {
            return issue.getPriorityScore();
        }

        return calculateCurrentPriorityScore(issue);
    }

    private String resolveThumbnailUrl(Issue issue) {
        return issue.getImages().stream()
                .filter(image -> !Boolean.TRUE.equals(image.getResolutionProof()))
                .min(Comparator.comparing(IssueImage::getDisplayOrder))
                .map(image -> issueImageStorageService.resolveAccessUrl(image.getStoragePath(), image.getPublicUrl()))
                .orElse(null);
    }

    private AdminIssueSeverityResponse toResponse(Issue issue) {
        Integer effectiveSeverity = issue.getVerifiedSeverity() != null
                ? issue.getVerifiedSeverity()
                : issue.getReportedSeverity();

        return AdminIssueSeverityResponse.builder()
                .issueId(issue.getId())
                .title(issue.getTitle())
                .reportedSeverity(issueSeverityMapper.toApiValue(issue.getReportedSeverity()))
                .verifiedSeverity(issueSeverityMapper.toApiValue(issue.getVerifiedSeverity()))
                .effectiveSeverity(issueSeverityMapper.toApiValue(effectiveSeverity))
                .priorityScore(issue.getPriorityScore())
                .build();
    }
}
