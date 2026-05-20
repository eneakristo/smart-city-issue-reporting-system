package com.smartcity.issuereporting.repository;

import com.smartcity.issuereporting.model.entity.Vote;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    Optional<Vote> findByIssueIdAndUserId(UUID issueId, UUID userId);

    boolean existsByIssueIdAndUserIdAndActiveTrue(UUID issueId, UUID userId);

    long countByIssueIdAndActiveTrue(UUID issueId);

    void deleteByIssueId(UUID issueId);
}
