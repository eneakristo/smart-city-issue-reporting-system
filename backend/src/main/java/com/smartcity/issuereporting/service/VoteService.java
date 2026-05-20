package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.dto.issue.VoteIssueResponse;
import com.smartcity.issuereporting.exception.BadRequestException;
import com.smartcity.issuereporting.exception.ResourceNotFoundException;
import com.smartcity.issuereporting.model.entity.Issue;
import com.smartcity.issuereporting.model.entity.User;
import com.smartcity.issuereporting.model.entity.Vote;
import com.smartcity.issuereporting.repository.IssueRepository;
import com.smartcity.issuereporting.repository.UserRepository;
import com.smartcity.issuereporting.repository.VoteRepository;
import com.smartcity.issuereporting.security.AuthUser;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoteService {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final VoteRepository voteRepository;
    private final IssuePrioritizationService issuePrioritizationService;

    public VoteService(
            IssueRepository issueRepository,
            UserRepository userRepository,
            VoteRepository voteRepository,
            IssuePrioritizationService issuePrioritizationService
    ) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.issuePrioritizationService = issuePrioritizationService;
    }

    @Transactional
    public VoteIssueResponse voteForIssue(java.util.UUID issueId, AuthUser authUser) {
        Issue issue = issueRepository.findWithDetailsById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

        if (issue.isDuplicate()) {
            throw new BadRequestException("Votes can only be added to the original issue");
        }

        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Vote vote = voteRepository.findByIssueIdAndUserId(issueId, user.getId())
                .orElseGet(() -> {
                    Vote newVote = new Vote();
                    newVote.setIssue(issue);
                    newVote.setUser(user);
                    return newVote;
                });

        vote.setActive(true);
        voteRepository.save(vote);

        return buildVoteResponse(issue, user.getId(), "added");
    }

    @Transactional
    public VoteIssueResponse removeVote(java.util.UUID issueId, AuthUser authUser) {
        Issue issue = issueRepository.findWithDetailsById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

        if (issue.isDuplicate()) {
            throw new BadRequestException("Votes can only be removed from the original issue");
        }

        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        Vote vote = voteRepository.findByIssueIdAndUserId(issueId, user.getId())
                .orElseThrow(() -> new BadRequestException("You have not voted for this issue"));

        vote.setActive(false);
        voteRepository.save(vote);

        return buildVoteResponse(issue, user.getId(), "removed");
    }

    private VoteIssueResponse buildVoteResponse(Issue issue, java.util.UUID userId, String action) {
        long voteCount = voteRepository.countByIssueIdAndActiveTrue(issue.getId());
        BigDecimal priorityScore = issuePrioritizationService.calculatePriorityScore(
                issue,
                voteCount,
                issue.getDuplicateCount(),
                Instant.now()
        );
        issue.setVoteCount(Math.toIntExact(voteCount));
        issue.setPriorityScore(priorityScore);
        issueRepository.save(issue);

        boolean currentUserHasVoted = voteRepository.existsByIssueIdAndUserIdAndActiveTrue(issue.getId(), userId);

        return VoteIssueResponse.builder()
                .issueId(issue.getId())
                .voteCount(voteCount)
                .currentUserHasVoted(currentUserHasVoted)
                .action(action)
                .priorityScore(priorityScore)
                .build();
    }
}
