package com.smartcity.issuereporting.repository;

import com.smartcity.issuereporting.model.entity.IssueStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueStatusHistoryRepository extends JpaRepository<IssueStatusHistory, UUID> {

    @EntityGraph(attributePaths = {"changedBy"})
    List<IssueStatusHistory> findByIssueIdOrderByCreatedAtAsc(UUID issueId);

    void deleteByIssueId(UUID issueId);
}
