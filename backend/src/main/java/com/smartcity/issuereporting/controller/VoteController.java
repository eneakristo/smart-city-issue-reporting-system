package com.smartcity.issuereporting.controller;

import com.smartcity.issuereporting.dto.issue.VoteIssueResponse;
import com.smartcity.issuereporting.security.AuthUser;
import com.smartcity.issuereporting.service.VoteService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issues/{issueId}/votes")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VoteIssueResponse voteForIssue(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return voteService.voteForIssue(issueId, authUser);
    }

    @DeleteMapping
    public VoteIssueResponse removeVote(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return voteService.removeVote(issueId, authUser);
    }
}
