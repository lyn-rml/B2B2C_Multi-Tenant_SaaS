package com.marketplace.repository;

import com.marketplace.domain.ProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ProcessRepository extends JpaRepository<ProcessEntity, Long> {

    @Query(value = """
            SELECT * FROM public.process
            WHERE tenant_id = :tenantId AND effective_date >= :dayStart AND effective_date < :dayEnd
            ORDER BY id LIMIT 1
            """, nativeQuery = true)
    Optional<ProcessEntity> findByEffectiveDateOnDay(
            @Param("tenantId") UUID tenantId,
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd
    );
}
