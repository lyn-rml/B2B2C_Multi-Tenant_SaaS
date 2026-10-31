package com.marketplace.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
public class TenantProvisioningService {
    private static final Set<String> SUPPORTED = Set.of("PRODUCTS_BASKETS", "STUDIOS", "ACTIVITIES");
    private final JdbcTemplate jdbc;
    public TenantProvisioningService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean supports(String code) { return SUPPORTED.contains(code); }

    // Caller must lock the tenant row before invoking this inside the assignment transaction.
    @Transactional(propagation = Propagation.MANDATORY)
    public void provision(String schema, List<String> modules) {
        if (!schema.matches("tenant_[a-z0-9_]+") || schema.length() > 63) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid tenant schema name");
        }
        String quoted = "\"" + schema + "\"";
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + quoted);
        jdbc.execute("CREATE TABLE IF NOT EXISTS " + quoted + """
                .schema_migrations (
                    module_code VARCHAR(40) NOT NULL,
                    version INTEGER NOT NULL,
                    checksum VARCHAR(64) NOT NULL,
                    applied_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                    PRIMARY KEY (module_code, version)
                )
                """);
        apply(quoted, "COMMON", 1);
        for (String code : modules) {
            if (!supports(code)) throw new ResponseStatusException(HttpStatus.CONFLICT, "No migrations for module " + code);
            apply(quoted, code, 1);
        }
    }

    private void apply(String schema, String code, int version) {
        byte[] bytes;
        String checksum;
        try {
            bytes = new ClassPathResource("db/tenant/" + code + "/V" + version + ".sql").getContentAsByteArray();
            checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot load migration " + code, ex);
        }
        List<String> applied = jdbc.queryForList("SELECT checksum FROM " + schema
                + ".schema_migrations WHERE module_code = ? AND version = ?", String.class, code, version);
        if (!applied.isEmpty()) {
            if (!applied.getFirst().equals(checksum)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Previously applied migration changed: " + code);
            }
            return;
        }
        byte[] sql = new String(bytes, StandardCharsets.UTF_8).replace("__tenant__", schema).getBytes(StandardCharsets.UTF_8);
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sql));
            return null;
        });
        jdbc.update("INSERT INTO " + schema + ".schema_migrations (module_code, version, checksum) VALUES (?, ?, ?)",
                code, version, checksum);
    }
}
