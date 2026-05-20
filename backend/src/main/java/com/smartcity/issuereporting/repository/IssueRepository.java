package com.smartcity.issuereporting.repository;

import com.smartcity.issuereporting.model.entity.Issue;
import com.smartcity.issuereporting.model.entity.IssueStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {

    @Override
    @EntityGraph(attributePaths = {"category", "createdByUser", "images", "parentIssue"})
    List<Issue> findAll(Specification<Issue> specification);

    @EntityGraph(attributePaths = {"category", "createdByUser", "images", "parentIssue"})
    Optional<Issue> findWithDetailsById(UUID issueId);

    long countByParentIssueId(UUID parentIssueId);

    @EntityGraph(attributePaths = {"category", "createdByUser", "images", "parentIssue"})
    List<Issue> findByParentIssueId(UUID parentIssueId);

    @EntityGraph(attributePaths = {"category", "createdByUser", "parentIssue"})
    List<Issue> findTop12ByStatusOrderByCreatedAtDesc(IssueStatus status);

    @EntityGraph(attributePaths = {"category", "createdByUser", "parentIssue"})
    List<Issue> findTop50ByStatusInOrderByCreatedAtDesc(Collection<IssueStatus> statuses);

    List<Issue> findTop20ByCategoryIdAndDuplicateFalseAndStatusInAndIdNotOrderByCreatedAtDesc(
            UUID categoryId,
            Collection<IssueStatus> statuses,
            UUID excludedIssueId
    );
}
