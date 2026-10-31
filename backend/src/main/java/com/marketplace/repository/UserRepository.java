package com.marketplace.repository;

import com.marketplace.domain.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @EntityGraph(attributePaths = {"roles", "roles.tenant"})
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles", "roles.tenant"})
    Optional<UserEntity> findWithRolesById(Long id);

    boolean existsByEmailIgnoreCase(String email);
}
