package com.financialcorp.cachepoc.cache;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cache.db-notify.enabled", havingValue = "true")
public class PostgresAccountNotifyInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PostgresAccountNotifyInitializer.class);

    private final DataSource dataSource;

    public PostgresAccountNotifyInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (var connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                return;
            }
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute(
                """
                CREATE OR REPLACE FUNCTION notify_account_changed() RETURNS trigger AS $$
                BEGIN
                  IF (TG_OP = 'DELETE') THEN
                    PERFORM pg_notify('account_changed', TG_OP || ':' || OLD.id);
                  ELSE
                    PERFORM pg_notify('account_changed', TG_OP || ':' || NEW.id);
                  END IF;
                  RETURN NULL;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbc.execute("DROP TRIGGER IF EXISTS accounts_cache_notify ON accounts");
        jdbc.execute(
                """
                CREATE TRIGGER accounts_cache_notify
                AFTER INSERT OR UPDATE OR DELETE ON accounts
                FOR EACH ROW EXECUTE FUNCTION notify_account_changed()
                """);
        log.info("Registered PostgreSQL NOTIFY trigger for accounts cache sync");
    }
}
