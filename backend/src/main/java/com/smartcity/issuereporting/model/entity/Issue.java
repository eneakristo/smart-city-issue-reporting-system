package com.smartcity.issuereporting.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "issues",
        indexes = {
                @Index(name = "idx_issues_created_by_user_id", columnList = "created_by_user_id"),
                @Index(name = "idx_issues_category_id", columnList = "category_id"),
                @Index(name = "idx_issues_status", columnList = "status"),
                @Index(name = "idx_issues_reported_severity", columnList = "reported_severity"),
                @Index(name = "idx_issues_verified_severity", columnList = "verified_severity"),
                @Index(name = "idx_issues_zone", columnList = "zone"),
                @Index(name = "idx_issues_is_duplicate", columnList = "is_duplicate"),
                @Index(name = "idx_issues_parent_issue_id", columnList = "parent_issue_id"),
                @Index(name = "idx_issues_category_status", columnList = "category_id, status"),
                @Index(name = "idx_issues_duplicate_status", columnList = "is_duplicate, status")
        }
)
public class Issue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "location_text", nullable = false, length = 255)
    private String locationText;

    @Column(name = "reported_severity", nullable = false, length = 20)
    private Integer reportedSeverity;

    @Column(name = "verified_severity", length = 20)
    private Integer verifiedSeverity;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CityZone zone;

    @Column(name = "priority_score", precision = 12, scale = 4)
    private BigDecimal priorityScore;

    @Column(name = "is_duplicate", nullable = false)
    private boolean duplicate;

    @Column(name = "duplicate_confidence", precision = 5, scale = 4)
    private BigDecimal duplicateConfidence;

    @Column(name = "vote_count", nullable = false)
    private int voteCount;

    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IssueStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_issue_id")
    private Issue parentIssue;

    @Column(name = "administrator_seen_at")
    private Instant administratorSeenAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrator_seen_by_id")
    private User administratorSeenBy;

    @OneToMany(mappedBy = "parentIssue", fetch = FetchType.LAZY)
    private List<Issue> duplicateIssues = new ArrayList<>();

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    private List<IssueImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    private List<Vote> votes = new ArrayList<>();

    @OneToMany(mappedBy = "duplicateIssue", fetch = FetchType.LAZY)
    private List<DuplicateReview> duplicateReviews = new ArrayList<>();

    @OneToMany(mappedBy = "issue", fetch = FetchType.LAZY)
    private List<IssueStatusHistory> statusHistory = new ArrayList<>();
}
