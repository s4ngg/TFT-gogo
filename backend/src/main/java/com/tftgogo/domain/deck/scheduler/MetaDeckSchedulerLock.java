package com.tftgogo.domain.deck.scheduler;

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
public class MetaDeckSchedulerLock {

    private static final Logger logger = LogManager.getLogger(MetaDeckSchedulerLock.class);
    private static final String LOCK_NAME = "tftgogo.meta-deck.scheduler.aggregate";
    private static final int LOCK_TIMEOUT_SECONDS = 0;

    private final DataSource dataSource;

    public boolean runWithLock(String trigger, Runnable task) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tryAcquire(connection)) {
                logger.info("메타 덱 집계 스킵 - 다른 인스턴스가 이미 scheduler DB 락을 보유 중. trigger={}", trigger);
                return false;
            }

            try {
                task.run();
                return true;
            } finally {
                release(connection, trigger);
            }
        } catch (SQLException e) {
            logger.error("메타 덱 scheduler DB 락 획득 실패. trigger={}", trigger, e);
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
                    logger.warn("메타 덱 scheduler DB 락이 정상적으로 해제되지 않았습니다. trigger={}", trigger);
                }
            }
        } catch (SQLException e) {
            logger.error("메타 덱 scheduler DB 락 해제 실패. trigger={}", trigger, e);
        }
    }
}
