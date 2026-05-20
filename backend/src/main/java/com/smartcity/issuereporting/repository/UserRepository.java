package com.smartcity.issuereporting.repository;

import com.smartcity.issuereporting.model.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    List<User> findAllByOrderByCreatedAtDesc();

    Optional<User> findByEmailIgnoreCase(String email);
}
