package com.financialcorp.cachepoc.cache;

import com.financialcorp.cachepoc.repository.AccountRepository;
import com.financialcorp.cachepoc.service.CacheSyncService;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cache.db-notify.enabled", havingValue = "true")
public class PostgresAccountChangeListener implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PostgresAccountChangeListener.class);

    private final AccountRepository accountRepository;
    private final CacheSyncService cacheSyncService;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread worker;

    public PostgresAccountChangeListener(
            AccountRepository accountRepository,
            CacheSyncService cacheSyncService,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {
        this.accountRepository = accountRepository;
        this.cacheSyncService = cacheSyncService;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.worker = new Thread(this, "pg-account-cache-notify");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void run() {
        if (jdbcUrl == null || !jdbcUrl.contains("postgresql")) {
            log.info("DB notify listener idle — datasource is not PostgreSQL");
            return;
        }
        while (running.get()) {
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                    Statement statement = connection.createStatement()) {
                statement.execute("LISTEN account_changed");
                PGConnection pgConnection = connection.unwrap(PGConnection.class);
                log.info("Listening for PostgreSQL account_changed notifications");
                while (running.get()) {
                    PGNotification[] notifications = pgConnection.getNotifications(5000);
                    if (notifications == null) {
                        continue;
                    }
                    for (PGNotification notification : notifications) {
                        handle(notification.getParameter());
                    }
                }
            } catch (Exception ex) {
                if (running.get()) {
                    log.warn("PostgreSQL notify listener reconnecting: {}", ex.getMessage());
                    sleepQuietly();
                }
            }
        }
    }

    private void handle(String payload) {
        if (payload == null || !payload.contains(":")) {
            return;
        }
        String[] parts = payload.split(":", 2);
        String op = parts[0];
        Long id = Long.valueOf(parts[1]);
        if ("DELETE".equals(op)) {
            cacheSyncService.evictAccount(id);
            return;
        }
        accountRepository.findById(id).ifPresentOrElse(
                cacheSyncService::putAccount,
                () -> cacheSyncService.evictAccount(id));
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        worker.interrupt();
    }
}
