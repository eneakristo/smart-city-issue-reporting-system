package com.smartcity.issuereporting.repository;

import com.smartcity.issuereporting.model.entity.DuplicateReview;
import com.smartcity.issuereporting.model.entity.DuplicateReviewDecision;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DuplicateReviewRepository extends JpaRepository<DuplicateReview, UUID> {

    @EntityGraph(attributePaths = {
            "duplicateIssue",
            "duplicateIssue.category",
            "duplicateIssue.images",
            "duplicateIssue.parentIssue",
            "duplicateIssue.parentIssue.category",
            "parentIssue",
            "parentIssue.category",
            "reviewedBy"
    })
    List<DuplicateReview> findAllByDecisionInOrderByCreatedAtDesc(Collection<DuplicateReviewDecision> decisions);

    void deleteByDuplicateIssueId(UUID duplicateIssueId);
}
