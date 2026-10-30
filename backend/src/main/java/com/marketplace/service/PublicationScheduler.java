package com.marketplace.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class PublicationScheduler {
    private final JdbcTemplate jdbc;

    public PublicationScheduler(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "${marketplace.publication-delay-ms:60000}",
            initialDelayString = "${marketplace.publication-delay-ms:60000}")
    public void refresh() {
        jdbc.execute("SELECT app.refresh_publications()");
    }
}
