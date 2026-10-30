package com.marketplace.service;

import com.marketplace.domain.ProcessEntity;
import com.marketplace.repository.ProcessRepository;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessServiceTest {
    @Test
    void publicationProcessIsLookedUpAndCreatedWithinTheRequestedTenant() {
        var repository = mock(ProcessRepository.class);
        UUID tenant = UUID.randomUUID();
        var date = OffsetDateTime.parse("2026-11-10T12:00:00Z");
        var start = OffsetDateTime.parse("2026-11-10T00:00:00Z");
        var end = OffsetDateTime.parse("2026-11-11T00:00:00Z");
        when(repository.findByEffectiveDateOnDay(tenant, start, end)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        ProcessEntity process = new ProcessService(repository).findOrCreateByDate(tenant, date);
        verify(repository).findByEffectiveDateOnDay(tenant, start, end);
        assertEquals(tenant, process.getTenantId());
        assertEquals(start, process.getEffectiveDate());
        assertEquals(end, process.getEndDate());
        assertTrue(process.isActive());
    }
}
