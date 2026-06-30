package com.tftgogo.domain.guide.scheduler;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class GuideCdragonImportSchedulerLock {

    private static final Logger logger = LogManager.getLogger(GuideCdragonImportSchedulerLock.class);
    private static final String LOCK_NAME = "tftgogo.guide.cdragon.scheduler.import";
    private static final int LOCK_TIMEOUT_SECONDS = 0;

    private final DataSource dataSource;

    public boolean runWithLock(String trigger, Runnable task) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tryAcquire(connection)) {
                logger.info("Guide CDragon import skipped because scheduler DB lock is already held. trigger={}", trigger);
                return false;
            }

            try {
                task.run();
                return true;
            } finally {
                release(connection, trigger);
            }
        } catch (SQLException e) {
            logger.error("Guide CDragon import skipped because scheduler DB lock failed. trigger={}", trigger, e);
            return false;
        }
    }

    private boolean tryAcquire(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, LOCK_NAME);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 1;
            }
        }
    }

    private void release(Connection connection, String trigger) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    logger.warn("Guide CDragon scheduler DB lock was not released normally. trigger={}", trigger);
                }
            }
        } catch (SQLException e) {
            logger.error("Guide CDragon scheduler DB lock release failed. trigger={}", trigger, e);
        }
    }
}
