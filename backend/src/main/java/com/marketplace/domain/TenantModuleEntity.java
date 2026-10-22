package com.marketplace.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "tenant_modules", schema = "public")
@Getter
@Setter
public class TenantModuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @Column(name = "module_key", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private ModuleKey moduleKey;

    @Column(name = "enabled_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime enabledAt;
}
