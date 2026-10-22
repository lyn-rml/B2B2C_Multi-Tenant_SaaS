package com.marketplace.repository;

import com.marketplace.domain.RoleType;
import com.marketplace.domain.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    @Query("""
            SELECT r FROM UserRoleEntity r
            JOIN FETCH r.user
            LEFT JOIN FETCH r.tenant
            ORDER BY r.user.email, r.role
            """)
    List<UserRoleEntity> findAllWithUserAndTenant();

    @Query("""
            SELECT r FROM UserRoleEntity r
            JOIN FETCH r.tenant
            WHERE r.user.id = :userId AND r.role = :role
            """)
    List<UserRoleEntity> findByUserIdAndRole(@Param("userId") Long userId, @Param("role") RoleType role);

    boolean existsByUser_IdAndRole(Long userId, RoleType role);

    boolean existsByUser_IdAndTenant_Id(Long userId, java.util.UUID tenantId);
}
