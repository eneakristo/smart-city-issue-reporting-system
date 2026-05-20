package com.smartcity.issuereporting.repository;

import com.smartcity.issuereporting.dto.issue.IssueQueryRequest;
import com.smartcity.issuereporting.model.entity.CityZone;
import com.smartcity.issuereporting.model.entity.Issue;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class IssueSpecifications {

    private IssueSpecifications() {
    }

    public static Specification<Issue> publicPrimaryIssues(IssueQueryRequest request) {
        return Specification.allOf(
                isPrimaryIssue(),
                matchesFilters(request),
                distinctResults()
        );
    }

    public static Specification<Issue> administratorPrimaryIssues(IssueQueryRequest request, CityZone assignedZone) {
        return Specification.allOf(
                isPrimaryIssue(),
                inZone(assignedZone),
                matchesFilters(request),
                distinctResults()
        );
    }

    public static Specification<Issue> issuesReportedBy(UUID reporterId, IssueQueryRequest request) {
        return Specification.allOf(
                reportedBy(reporterId),
                matchesFilters(request),
                distinctResults()
        );
    }

    private static Specification<Issue> isPrimaryIssue() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("duplicate"));
    }

    private static Specification<Issue> reportedBy(UUID reporterId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("createdByUser").get("id"), reporterId);
    }

    private static Specification<Issue> inZone(CityZone zone) {
        return (root, query, criteriaBuilder) -> {
            if (zone == null) {
                return criteriaBuilder.disjunction();
            }

            return criteriaBuilder.equal(root.get("zone"), zone);
        };
    }

    private static Specification<Issue> matchesFilters(IssueQueryRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (request.getCategory() != null && !request.getCategory().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category").get("name")),
                        request.getCategory().trim().toLowerCase()
                ));
            }

            if (request.getParsedStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getParsedStatus()));
            }

            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String keywordPattern = likePattern(request.getKeyword());
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), keywordPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), keywordPattern)
                ));
            }

            if (request.getLocationText() != null && !request.getLocationText().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("locationText")),
                        likePattern(request.getLocationText())
                ));
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static Specification<Issue> distinctResults() {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            return criteriaBuilder.conjunction();
        };
    }

    private static String likePattern(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
