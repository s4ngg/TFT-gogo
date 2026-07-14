package com.tftgogo.domain.content.scheduler;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
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
public class ContentRefreshSchedulerLock {

    private static final Logger logger = LogManager.getLogger(ContentRefreshSchedulerLock.class);
    private static final String LOCK_NAME = "tftgogo.content.refresh.scheduler.import";
    private static final int LOCK_TIMEOUT_SECONDS = 0;

    private final DataSource dataSource;

    public boolean runWithLock(String trigger, Runnable task) {
        try (Connection connection = dataSource.getConnection()) {
            boolean acquired;
            try {
                acquired = tryAcquire(connection);
            } catch (SQLException e) {
                abortConnection(connection, trigger);
                throw e;
            }
            if (!acquired) {
                logger.info("Content refresh skipped because scheduler DB lock is already held. trigger={}", trigger);
                return false;
            }
            logger.info("Content refresh scheduler DB lock acquired. trigger={}", trigger);

            try {
                task.run();
                return true;
            } finally {
                release(connection, trigger);
            }
        } catch (SQLException e) {
            logger.error("Content refresh scheduler DB lock failed. trigger={}", trigger, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private boolean tryAcquire(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, LOCK_NAME);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("GET_LOCK returned no result");
                }
                int result = resultSet.getInt(1);
                if (resultSet.wasNull()) {
                    throw new SQLException("GET_LOCK returned NULL");
                }
                if (result != 0 && result != 1) {
                    throw new SQLException("GET_LOCK returned unexpected result: " + result);
                }
                return result == 1;
            }
        }
    }

    private void release(Connection connection, String trigger) {
        boolean released = false;
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                released = resultSet.next() && resultSet.getInt(1) == 1;
            }
        } catch (SQLException e) {
            logger.error("Content refresh scheduler DB lock release failed. trigger={}", trigger, e);
        }

        if (released) {
            logger.info("Content refresh scheduler DB lock released. trigger={}", trigger);
            return;
        }

        logger.warn(
                "Content refresh scheduler DB lock was not released normally. "
                        + "The lock connection will be aborted. trigger={}",
                trigger
        );
        abortConnection(connection, trigger);
    }

    private void abortConnection(Connection connection, String trigger) {
        try {
            connection.abort(Runnable::run);
        } catch (SQLException e) {
            logger.error("Content refresh scheduler lock connection abort failed. trigger={}", trigger, e);
        }
    }
}
