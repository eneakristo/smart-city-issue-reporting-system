package com.smartcity.issuereporting.controller;

import com.smartcity.issuereporting.dto.systemadmin.SystemAdminCreateUserRequest;
import com.smartcity.issuereporting.dto.systemadmin.SystemAdminUpdateUserRoleRequest;
import com.smartcity.issuereporting.dto.systemadmin.SystemAdminUserResponse;
import com.smartcity.issuereporting.security.AuthUser;
import com.smartcity.issuereporting.service.SystemAdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system-admin")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemAdminController {

    private final SystemAdminUserService systemAdminUserService;

    public SystemAdminController(SystemAdminUserService systemAdminUserService) {
        this.systemAdminUserService = systemAdminUserService;
    }

    @GetMapping("/users")
    public List<SystemAdminUserResponse> listUsers() {
        return systemAdminUserService.listUsers();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public SystemAdminUserResponse createUser(@Valid @RequestBody SystemAdminCreateUserRequest request) {
        return systemAdminUserService.createUser(request);
    }

    @PatchMapping("/users/{userId}/role")
    public SystemAdminUserResponse updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody SystemAdminUpdateUserRoleRequest request,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return systemAdminUserService.updateUserRole(userId, request, authUser);
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeUser(@PathVariable UUID userId, @AuthenticationPrincipal AuthUser authUser) {
        systemAdminUserService.removeUser(userId, authUser);
    }
}
