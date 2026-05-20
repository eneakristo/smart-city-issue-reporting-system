package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.dto.systemadmin.SystemAdminCreateUserRequest;
import com.smartcity.issuereporting.dto.systemadmin.SystemAdminUpdateUserRoleRequest;
import com.smartcity.issuereporting.dto.systemadmin.SystemAdminUserResponse;
import com.smartcity.issuereporting.exception.BadRequestException;
import com.smartcity.issuereporting.exception.EmailAlreadyExistsException;
import com.smartcity.issuereporting.exception.ResourceNotFoundException;
import com.smartcity.issuereporting.model.entity.CityZone;
import com.smartcity.issuereporting.model.entity.Role;
import com.smartcity.issuereporting.model.entity.User;
import com.smartcity.issuereporting.repository.UserRepository;
import com.smartcity.issuereporting.security.AuthUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemAdminUserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public SystemAdminUserService(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SystemAdminUserResponse> listUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SystemAdminUserResponse createUser(SystemAdminCreateUserRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (request.getRole() == Role.ADMIN || request.getRole() == Role.SYSTEM_ADMIN) {
            throw new BadRequestException("System admins can create only citizen and administrator accounts");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setAssignedZone(resolveAssignedZone(request.getRole(), request.getAssignedZone()));
        user.setActive(true);

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void removeUser(UUID userId, AuthUser authUser) {
        if (authUser.getId().equals(userId)) {
            throw new BadRequestException("System administrators cannot remove their own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public SystemAdminUserResponse updateUserRole(
            UUID userId,
            SystemAdminUpdateUserRoleRequest request,
            AuthUser authUser
    ) {
        if (authUser.getId().equals(userId)) {
            throw new BadRequestException("System administrators cannot change their own role");
        }

        if (request.getRole() != Role.CITIZEN && request.getRole() != Role.ADMINISTRATOR) {
            throw new BadRequestException("Role can be changed only to citizen or administrator");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BadRequestException("Removed accounts cannot be updated");
        }

        if (user.getRole() == Role.SYSTEM_ADMIN) {
            throw new BadRequestException("System administrator roles cannot be changed here");
        }

        user.setRole(request.getRole());
        user.setAssignedZone(resolveAssignedZone(request.getRole(), request.getAssignedZone()));
        return toResponse(userRepository.save(user));
    }

    private CityZone resolveAssignedZone(Role role, CityZone requestedZone) {
        if (role != Role.ADMINISTRATOR) {
            return null;
        }

        return requestedZone == null ? CityZone.ZONE_1 : requestedZone;
    }

    private SystemAdminUserResponse toResponse(User user) {
        return SystemAdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .assignedZone(user.getAssignedZone())
                .active(Boolean.TRUE.equals(user.getActive()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
