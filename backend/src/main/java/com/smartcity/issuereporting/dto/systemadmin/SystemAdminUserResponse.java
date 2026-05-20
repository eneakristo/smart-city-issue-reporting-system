package com.smartcity.issuereporting.dto.systemadmin;

import com.smartcity.issuereporting.model.entity.Role;
import com.smartcity.issuereporting.model.entity.CityZone;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SystemAdminUserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private Role role;
    private CityZone assignedZone;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
