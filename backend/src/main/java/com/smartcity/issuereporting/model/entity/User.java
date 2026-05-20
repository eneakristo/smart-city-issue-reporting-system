package com.smartcity.issuereporting.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_role", columnList = "role")
        }
)
public class User extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 30)
    private String phoneNumber;

    @Column(length = 600)
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_zone", length = 20)
    private CityZone assignedZone;

    @Column
    private Boolean active = true;

    @OneToMany(mappedBy = "createdByUser", fetch = FetchType.LAZY)
    private List<Issue> reportedIssues = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Vote> votes = new ArrayList<>();

    @OneToMany(mappedBy = "reviewedBy", fetch = FetchType.LAZY)
    private List<DuplicateReview> duplicateReviews = new ArrayList<>();

    @OneToMany(mappedBy = "changedBy", fetch = FetchType.LAZY)
    private List<IssueStatusHistory> statusChanges = new ArrayList<>();
}
