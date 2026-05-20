package com.smartcity.issuereporting.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "duplicate_reviews",
        indexes = {
                @Index(name = "idx_duplicate_reviews_issue_id", columnList = "duplicate_issue_id"),
                @Index(name = "idx_duplicate_reviews_parent_issue_id", columnList = "parent_issue_id"),
                @Index(name = "idx_duplicate_reviews_reviewed_by", columnList = "reviewed_by_admin_id"),
                @Index(name = "idx_duplicate_reviews_decision", columnList = "decision")
        }
)
public class DuplicateReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "duplicate_issue_id", nullable = false)
    private Issue duplicateIssue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_issue_id", nullable = false)
    private Issue parentIssue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DuplicateReviewDecision decision;

    @Column(name = "duplicate_confidence", precision = 5, scale = 4)
    private BigDecimal duplicateConfidence;

    @Column(name = "match_reason", length = 1000)
    private String matchReason;

    @Column(length = 1000)
    private String reviewNotes;
}
