package com.it342.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Configuration
public class AdaptiveDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveDataSourceConfig.class);
    private static final String DEFAULT_OFFLINE_URL =
            "jdbc:h2:file:../.data/tradeoff;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_ON_EXIT=FALSE";

    @Bean(destroyMethod = "close")
    @Primary
    public DataSource dataSource(Environment environment) {
        String onlineUrl = valueOrEmpty(environment.getProperty("spring.datasource.url"));
        String onlineUsername = valueOrEmpty(environment.getProperty("spring.datasource.username"));
        String onlinePassword = environment.getProperty("spring.datasource.password", "");

        String offlineUrl = environment.getProperty("app.datasource.offline.url", DEFAULT_OFFLINE_URL);
        String offlineUsername = environment.getProperty("app.datasource.offline.username", "sa");
        String offlinePassword = environment.getProperty("app.datasource.offline.password", "");
        int onlineConnectTimeoutMs = environment.getProperty(
                "app.datasource.online-connect-timeout-ms",
                Integer.class,
                2500
        );

        if (isOnlineReachable(onlineUrl, onlineUsername, onlinePassword, onlineConnectTimeoutMs)) {
            log.info("Database mode: ONLINE (PostgreSQL)");
            return buildDataSource(
                    "tradeoff-online",
                    onlineUrl,
                    onlineUsername,
                    onlinePassword,
                    null,
                    onlineConnectTimeoutMs
            );
        }

        log.warn("Database mode: OFFLINE (local H2 file DB)");
        return buildDataSource(
                "tradeoff-offline",
                offlineUrl,
                offlineUsername,
                offlinePassword,
                "org.h2.Driver",
                Math.max(onlineConnectTimeoutMs, 3000)
        );
    }

    private boolean isOnlineReachable(String jdbcUrl, String username, String password, int connectTimeoutMs) {
        if (jdbcUrl.isBlank()) {
            log.warn("No online datasource URL configured. Starting in offline mode.");
            return false;
        }

        HikariDataSource probeDataSource = null;
        try {
            probeDataSource = buildDataSource(
                    "tradeoff-online-probe",
                    jdbcUrl,
                    username,
                    password,
                    null,
                    connectTimeoutMs
            );
            try (Connection ignored = probeDataSource.getConnection()) {
                return true;
            }
        } catch (SQLException ex) {
            log.warn("Online database unavailable: {}", ex.getMessage());
            return false;
        } finally {
            if (probeDataSource != null) {
                probeDataSource.close();
            }
        }
    }

    private HikariDataSource buildDataSource(
            String poolName,
            String jdbcUrl,
            String username,
            String password,
            String driverClassName,
            int connectTimeoutMs
    ) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setJdbcUrl(jdbcUrl);
        if (!username.isBlank()) {
            config.setUsername(username);
        }
        config.setPassword(password);
        if (driverClassName != null && !driverClassName.isBlank()) {
            config.setDriverClassName(driverClassName);
        }
        config.setConnectionTimeout(Math.max(connectTimeoutMs, 1000));
        config.setValidationTimeout(1000);
        config.setInitializationFailTimeout(0);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        return new HikariDataSource(config);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
