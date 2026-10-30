package com.marketplace.service;

import com.marketplace.domain.ProcessEntity;
import com.marketplace.repository.ProcessRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ProcessService {

    private final ProcessRepository processRepository;

    public ProcessService(ProcessRepository processRepository) {
        this.processRepository = processRepository;
    }

    @Transactional
    public ProcessEntity findOrCreateByDate(UUID tenantId, OffsetDateTime effectiveDate) {
        LocalDate day = effectiveDate.toLocalDate();
        OffsetDateTime dayStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime dayEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        return processRepository.findByEffectiveDateOnDay(tenantId, dayStart, dayEnd)
                .orElseGet(() -> {
                    ProcessEntity process = new ProcessEntity();
                    process.setTenantId(tenantId);
                    process.setName("Publication " + day);
                    process.setEffectiveDate(dayStart);
                    process.setEndDate(dayEnd);
                    process.setActive(true);
                    return processRepository.save(process);
                });
    }
}
