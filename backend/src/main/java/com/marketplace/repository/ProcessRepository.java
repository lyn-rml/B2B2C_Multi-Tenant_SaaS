package com.marketplace.repository;

import com.marketplace.domain.ProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ProcessRepository extends JpaRepository<ProcessEntity, Long> {

    @Query("""
            SELECT p FROM ProcessEntity p
            WHERE p.effectiveDate >= :dayStart AND p.effectiveDate < :dayEnd
            """)
    Optional<ProcessEntity> findByEffectiveDateOnDay(
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd
    );
}
