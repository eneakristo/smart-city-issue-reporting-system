package com.smartcity.issuereporting.repository;

import com.smartcity.issuereporting.model.entity.IssueImage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueImageRepository extends JpaRepository<IssueImage, UUID> {

    List<IssueImage> findByIssueIdOrderByDisplayOrderAsc(UUID issueId);

    List<IssueImage> findByIssueIdAndResolutionProofTrueOrderByDisplayOrderAsc(UUID issueId);

    void deleteByIssueId(UUID issueId);
}
