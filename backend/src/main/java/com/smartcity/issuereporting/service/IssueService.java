package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.config.ApplicationProperties;
import com.smartcity.issuereporting.dto.issue.CreateIssueRequest;
import com.smartcity.issuereporting.dto.issue.CreateIssueResponse;
import com.smartcity.issuereporting.dto.issue.IssueDetailsResponse;
import com.smartcity.issuereporting.dto.issue.IssueImageResponse;
import com.smartcity.issuereporting.dto.issue.IssueListSort;
import com.smartcity.issuereporting.dto.issue.IssueQueryRequest;
import com.smartcity.issuereporting.dto.issue.IssueSummaryResponse;
import com.smartcity.issuereporting.dto.issue.IssueStatusHistoryResponse;
import com.smartcity.issuereporting.exception.BadRequestException;
import com.smartcity.issuereporting.exception.ResourceNotFoundException;
import com.smartcity.issuereporting.model.entity.Category;
import com.smartcity.issuereporting.model.entity.DuplicateReview;
import com.smartcity.issuereporting.model.entity.DuplicateReviewDecision;
import com.smartcity.issuereporting.model.entity.Issue;
import com.smartcity.issuereporting.model.entity.IssueImage;
import com.smartcity.issuereporting.model.entity.IssueStatus;
import com.smartcity.issuereporting.model.entity.IssueStatusHistory;
import com.smartcity.issuereporting.model.entity.Role;
import com.smartcity.issuereporting.model.entity.User;
import com.smartcity.issuereporting.repository.CategoryRepository;
import com.smartcity.issuereporting.repository.DuplicateReviewRepository;
import com.smartcity.issuereporting.repository.IssueImageRepository;
import com.smartcity.issuereporting.repository.IssueRepository;
import com.smartcity.issuereporting.repository.IssueSpecifications;
import com.smartcity.issuereporting.repository.IssueStatusHistoryRepository;
import com.smartcity.issuereporting.repository.UserRepository;
import com.smartcity.issuereporting.repository.VoteRepository;
import com.smartcity.issuereporting.security.AuthUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class IssueService {

    private static final BigDecimal NON_PRIORITY_DUPLICATE_SCORE = BigDecimal.ZERO;

    private final IssueRepository issueRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final IssueImageRepository issueImageRepository;
    private final IssueStatusHistoryRepository issueStatusHistoryRepository;
    private final DuplicateReviewRepository duplicateReviewRepository;
    private final DuplicateDetectionService duplicateDetectionService;
    private final IssueImageStorageService issueImageStorageService;
    private final ApplicationProperties applicationProperties;
    private final VoteRepository voteRepository;
    private final LocationGeocodingService locationGeocodingService;
    private final IssuePrioritizationService issuePrioritizationService;
    private final IssueSeverityMapper issueSeverityMapper;
    private final CityZoneService cityZoneService;

    public IssueService(
            IssueRepository issueRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            IssueImageRepository issueImageRepository,
            IssueStatusHistoryRepository issueStatusHistoryRepository,
            DuplicateReviewRepository duplicateReviewRepository,
            DuplicateDetectionService duplicateDetectionService,
            IssueImageStorageService issueImageStorageService,
            ApplicationProperties applicationProperties,
            VoteRepository voteRepository,
            LocationGeocodingService locationGeocodingService,
            IssuePrioritizationService issuePrioritizationService,
            IssueSeverityMapper issueSeverityMapper,
            CityZoneService cityZoneService
    ) {
        this.issueRepository = issueRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.issueImageRepository = issueImageRepository;
        this.issueStatusHistoryRepository = issueStatusHistoryRepository;
        this.duplicateReviewRepository = duplicateReviewRepository;
        this.duplicateDetectionService = duplicateDetectionService;
        this.issueImageStorageService = issueImageStorageService;
        this.applicationProperties = applicationProperties;
        this.voteRepository = voteRepository;
        this.locationGeocodingService = locationGeocodingService;
        this.issuePrioritizationService = issuePrioritizationService;
        this.issueSeverityMapper = issueSeverityMapper;
        this.cityZoneService = cityZoneService;
    }

    @Transactional(readOnly = true)
    public List<IssueSummaryResponse> listPrimaryIssues(IssueQueryRequest request, AuthUser authUser) {
        List<Issue> issues = issueRepository.findAll(IssueSpecifications.publicPrimaryIssues(request));
        return sortIssues(issues, request.getParsedSort()).stream()
                .map(issue -> toIssueSummaryResponse(issue, authUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public IssueDetailsResponse getIssueById(java.util.UUID issueId, AuthUser authUser) {
        Issue issue = issueRepository.findWithDetailsById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

        ensureZoneAccessForAdministrator(issue, authUser);

        return toIssueDetailsResponse(issue, authUser);
    }

    @Transactional(readOnly = true)
    public List<IssueSummaryResponse> listCurrentUserIssues(IssueQueryRequest request, AuthUser authUser) {
        List<Issue> issues = issueRepository.findAll(IssueSpecifications.issuesReportedBy(authUser.getId(), request));
        return sortIssues(issues, request.getParsedSort()).stream()
                .map(issue -> toIssueSummaryResponse(issue, authUser))
                .toList();
    }

    @Transactional
    public CreateIssueResponse createIssue(CreateIssueRequest request, List<MultipartFile> images, AuthUser authUser) {
        validateImages(images);

        User createdByUser = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        Category category = categoryRepository.findByNameIgnoreCase(request.getCategory().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategory()));

        Issue issue = new Issue();
        issue.setCreatedByUser(createdByUser);
        issue.setCategory(category);
        issue.setTitle(request.getTitle().trim());
        issue.setDescription(request.getDescription().trim());
        issue.setLocationText(request.getLocationText().trim());
        applyResolvedCoordinates(issue, request);
        issue.setZone(cityZoneService.resolveZone(issue.getLongitude()));
        issue.setReportedSeverity(issueSeverityMapper.toDatabaseValue(request.getSeverity()));
        issue.setStatus(IssueStatus.REPORTED);
        issue.setDuplicate(false);
        issue.setVoteCount(0);
        issue.setDuplicateCount(0);
        issue.setPriorityScore(calculateCurrentPriorityScore(issue));

        Issue savedIssue = issueRepository.save(issue);
        List<IssueImage> savedImages = saveImages(savedIssue, images);

        DuplicateDetectionService.DuplicateDetectionResult detectionResult =
                duplicateDetectionService.detectPotentialDuplicates(savedIssue);

        if (detectionResult.reviewRequired()) {
            savedIssue.setDuplicate(true);
            savedIssue.setStatus(IssueStatus.UNDER_REVIEW);
            savedIssue.setParentIssue(detectionResult.linkedParentIssue());
            savedIssue.setDuplicateConfidence(toStoredConfidence(detectionResult.confidence()));
            savedIssue.setPriorityScore(NON_PRIORITY_DUPLICATE_SCORE);

            DuplicateReview duplicateReview = new DuplicateReview();
            duplicateReview.setDuplicateIssue(savedIssue);
            duplicateReview.setParentIssue(detectionResult.linkedParentIssue());
            duplicateReview.setDecision(DuplicateReviewDecision.PENDING);
            duplicateReview.setDuplicateConfidence(toStoredConfidence(detectionResult.confidence()));
            duplicateReview.setMatchReason(detectionResult.matchReason());
            duplicateReview.setReviewNotes(buildDuplicateReviewNotes(detectionResult));
            duplicateReviewRepository.save(duplicateReview);
        }

        Issue finalizedIssue = issueRepository.save(savedIssue);
        if (detectionResult.reviewRequired() && detectionResult.linkedParentIssue() != null) {
            refreshPrimaryIssuePriority(detectionResult.linkedParentIssue());
            issueRepository.save(detectionResult.linkedParentIssue());
        }
        saveInitialStatusHistory(finalizedIssue, createdByUser);

        return CreateIssueResponse.builder()
                .issueId(finalizedIssue.getId())
                .title(finalizedIssue.getTitle())
                .category(finalizedIssue.getCategory().getName())
                .severity(issueSeverityMapper.toApiValue(finalizedIssue.getReportedSeverity()))
                .status(finalizedIssue.getStatus().name())
                .issueType(toIssueTypeLabel(finalizedIssue))
                .locationText(finalizedIssue.getLocationText())
                .zone(toZoneLabel(finalizedIssue))
                .reporterId(createdByUser.getId())
                .duplicateReviewRequired(detectionResult.reviewRequired())
                .linkedParentIssueId(
                        detectionResult.linkedParentIssue() == null ? null : detectionResult.linkedParentIssue().getId()
                )
                .duplicateCandidates(detectionResult.candidates())
                .images(savedImages.stream().map(this::toImageResponse).toList())
                .createdAt(finalizedIssue.getCreatedAt())
                .build();
    }

    private void validateImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new BadRequestException("At least one image is required");
        }
    }

    private void applyResolvedCoordinates(Issue issue, CreateIssueRequest request) {
        if (request.getLatitude() != null && request.getLongitude() != null) {
            issue.setLatitude(request.getLatitude());
            issue.setLongitude(request.getLongitude());
            return;
        }

        locationGeocodingService.resolveCoordinates(request.getLocationText())
                .ifPresent(resolvedCoordinates -> {
                    issue.setLatitude(resolvedCoordinates.latitude());
                    issue.setLongitude(resolvedCoordinates.longitude());
                });
    }

    private List<IssueImage> saveImages(Issue issue, List<MultipartFile> images) {
        String issueStoragePrefix = applicationPrefixForIssue(issue.getId().toString());

        for (int index = 0; index < images.size(); index++) {
            MultipartFile image = images.get(index);
            IssueImageStorageService.StoredIssueImage storedImage =
                    issueImageStorageService.store(issueStoragePrefix, image);

            IssueImage issueImage = new IssueImage();
            issueImage.setIssue(issue);
            issueImage.setStoragePath(storedImage.storagePath());
            issueImage.setPublicUrl(storedImage.publicUrl());
            issueImage.setOriginalFileName(storedImage.originalFileName());
            issueImage.setContentType(storedImage.contentType());
            issueImage.setFileSizeBytes(storedImage.fileSizeBytes());
            issueImage.setDisplayOrder(index);
            issueImageRepository.save(issueImage);
        }

        return issueImageRepository.findByIssueIdOrderByDisplayOrderAsc(issue.getId());
    }

    private void saveInitialStatusHistory(Issue issue, User reporter) {
        IssueStatusHistory statusHistory = new IssueStatusHistory();
        statusHistory.setIssue(issue);
        statusHistory.setChangedBy(reporter);
        statusHistory.setOldStatus(null);
        statusHistory.setNewStatus(issue.getStatus());
        statusHistory.setNote("Issue created");
        issueStatusHistoryRepository.save(statusHistory);
    }

    private String buildDuplicateReviewNotes(DuplicateDetectionService.DuplicateDetectionResult detectionResult) {
        if (detectionResult.linkedParentIssue() == null) {
            return "Automatic duplicate review triggered";
        }

        return "Automatic duplicate review triggered against issue #%s with confidence %.2f. %s"
                .formatted(
                        detectionResult.linkedParentIssue().getId(),
                        detectionResult.confidence(),
                        detectionResult.matchReason()
                );
    }

    private BigDecimal toStoredConfidence(double confidence) {
        return BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP);
    }

    private String applicationPrefixForIssue(String issueId) {
        return applicationProperties.getStorage().getIssueImageObjectPrefix() + "/" + issueId;
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

    private List<Issue> sortIssues(List<Issue> issues, IssueListSort sort) {
        Comparator<Issue> comparator = Comparator.comparing(Issue::getCreatedAt, Comparator.reverseOrder());
        if (sort == IssueListSort.MOST_VOTED) {
            comparator = Comparator
                    .comparingLong(this::activeVoteCount).reversed()
                    .thenComparing(Issue::getCreatedAt, Comparator.reverseOrder());
        } else if (sort == IssueListSort.HIGHEST_PRIORITY) {
            comparator = Comparator
                    .comparing(this::calculateCurrentPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Issue::getCreatedAt, Comparator.reverseOrder());
        }

        return issues.stream()
                .sorted(comparator)
                .toList();
    }

    private IssueSummaryResponse toIssueSummaryResponse(Issue issue, AuthUser authUser) {
        return IssueSummaryResponse.builder()
                .issueId(issue.getId())
                .title(issue.getTitle())
                .category(issue.getCategory().getName())
                .severity(issueSeverityMapper.toApiValue(issue.getReportedSeverity()))
                .status(issue.getStatus().name())
                .issueType(toIssueTypeLabel(issue))
                .locationText(issue.getLocationText())
                .zone(toZoneLabel(issue))
                .thumbnailUrl(resolveThumbnailUrl(issue))
                .voteCount(activeVoteCount(issue))
                .currentUserHasVoted(hasCurrentUserVoted(issue, authUser))
                .priorityScore(calculateCurrentPriorityScore(issue))
                .parentIssueId(issue.getParentIssue() == null ? null : issue.getParentIssue().getId())
                .parentIssueTitle(issue.getParentIssue() == null ? null : issue.getParentIssue().getTitle())
                .createdAt(issue.getCreatedAt())
                .build();
    }

    private IssueDetailsResponse toIssueDetailsResponse(Issue issue, AuthUser authUser) {
        return IssueDetailsResponse.builder()
                .issueId(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .category(issue.getCategory().getName())
                .severity(issueSeverityMapper.toApiValue(issue.getReportedSeverity()))
                .status(issue.getStatus().name())
                .issueType(toIssueTypeLabel(issue))
                .locationText(issue.getLocationText())
                .latitude(issue.getLatitude())
                .longitude(issue.getLongitude())
                .zone(toZoneLabel(issue))
                .priorityScore(calculateCurrentPriorityScore(issue))
                .voteCount(activeVoteCount(issue))
                .currentUserHasVoted(hasCurrentUserVoted(issue, authUser))
                .reporterId(issue.getCreatedByUser().getId())
                .reporterName(issue.getCreatedByUser().getFullName())
                .parentIssueId(issue.getParentIssue() == null ? null : issue.getParentIssue().getId())
                .linkedDuplicateCount(countLinkedDuplicates(issue))
                .images(issue.getImages().stream()
                        .filter(image -> !Boolean.TRUE.equals(image.getResolutionProof()))
                        .sorted(Comparator.comparing(IssueImage::getDisplayOrder))
                        .map(this::toImageResponse)
                        .toList())
                .resolutionImages(issue.getImages().stream()
                        .filter(image -> Boolean.TRUE.equals(image.getResolutionProof()))
                        .sorted(Comparator.comparing(IssueImage::getDisplayOrder))
                        .map(this::toImageResponse)
                        .toList())
                .statusHistory(issueStatusHistoryRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId()).stream()
                        .map(this::toStatusHistoryResponse)
                        .toList())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }

    private IssueStatusHistoryResponse toStatusHistoryResponse(IssueStatusHistory statusHistory) {
        return IssueStatusHistoryResponse.builder()
                .historyId(statusHistory.getId())
                .oldStatus(statusHistory.getOldStatus() == null ? null : statusHistory.getOldStatus().name())
                .newStatus(statusHistory.getNewStatus().name())
                .changedByUserId(statusHistory.getChangedBy().getId())
                .changedByName(statusHistory.getChangedBy().getFullName())
                .note(statusHistory.getNote())
                .changedAt(statusHistory.getCreatedAt())
                .build();
    }

    private long activeVoteCount(Issue issue) {
        return issue.getVoteCount();
    }

    private String resolveThumbnailUrl(Issue issue) {
        return issue.getImages().stream()
                .min(Comparator.comparing(IssueImage::getDisplayOrder))
                .map(image -> issueImageStorageService.resolveAccessUrl(image.getStoragePath(), image.getPublicUrl()))
                .orElse(null);
    }

    private long countLinkedDuplicates(Issue issue) {
        if (issue.isDuplicate()) {
            return 0;
        }

        return issueRepository.countByParentIssueId(issue.getId());
    }

    private boolean hasCurrentUserVoted(Issue issue, AuthUser authUser) {
        if (authUser == null) {
            return false;
        }

        return voteRepository.existsByIssueIdAndUserIdAndActiveTrue(issue.getId(), authUser.getId());
    }

    private void ensureZoneAccessForAdministrator(Issue issue, AuthUser authUser) {
        if (authUser == null || authUser.getRole() == Role.SYSTEM_ADMIN || authUser.getRole() == Role.CITIZEN) {
            return;
        }

        User administrator = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated administrator not found"));

        if (!cityZoneService.canAdministratorAccessIssue(administrator.getAssignedZone(), issue)) {
            throw new ResourceNotFoundException("Issue not found: " + issue.getId());
        }
    }

    private String toIssueTypeLabel(Issue issue) {
        return issue.isDuplicate() ? "DUPLICATE" : "PRIMARY";
    }

    private String toZoneLabel(Issue issue) {
        return cityZoneService.resolveZone(issue) == null ? null : cityZoneService.resolveZone(issue).name();
    }

    private BigDecimal calculateCurrentPriorityScore(Issue issue) {
        return issuePrioritizationService.calculatePriorityScore(
                issue,
                activeVoteCount(issue),
                Math.toIntExact(countLinkedDuplicates(issue)),
                Instant.now()
        );
    }

    private void refreshPrimaryIssuePriority(Issue issue) {
        if (issue == null || issue.isDuplicate()) {
            return;
        }

        issue.setDuplicateCount(Math.toIntExact(issueRepository.countByParentIssueId(issue.getId())));
        issue.setPriorityScore(calculateCurrentPriorityScore(issue));
    }
}
