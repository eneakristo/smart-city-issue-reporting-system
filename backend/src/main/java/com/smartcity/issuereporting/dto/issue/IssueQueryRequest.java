package com.smartcity.issuereporting.dto.issue;

import com.smartcity.issuereporting.exception.BadRequestException;
import com.smartcity.issuereporting.model.entity.IssueStatus;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueQueryRequest {
    private String category;
    private String status;
    private String keyword;
    private String locationText;
    private String sort;

    public IssueStatus getParsedStatus() {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return IssueStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Status must be one of REPORTED, UNDER_REVIEW, CONFIRMED, IN_PROGRESS, RESOLVED, REJECTED");
        }
    }

    public IssueListSort getParsedSort() {
        if (sort == null || sort.isBlank()) {
            return IssueListSort.NEWEST;
        }

        String normalizedSort = sort.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);

        try {
            return IssueListSort.valueOf(normalizedSort);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Sort must be one of NEWEST, MOST_VOTED, HIGHEST_PRIORITY");
        }
    }
}
