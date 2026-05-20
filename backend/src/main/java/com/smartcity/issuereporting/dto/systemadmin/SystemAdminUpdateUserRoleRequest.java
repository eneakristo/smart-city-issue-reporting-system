package com.smartcity.issuereporting.dto.systemadmin;

import com.smartcity.issuereporting.model.entity.CityZone;
import com.smartcity.issuereporting.model.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemAdminUpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;

    private CityZone assignedZone;
}
