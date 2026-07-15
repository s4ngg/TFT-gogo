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
        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("메타 덱 scheduler DB 커넥션 획득 실패. trigger={}", trigger, e);
            return false;
        }

        boolean lockAcquired;
        try {
            lockAcquired = tryAcquire(connection);
        } catch (SQLException e) {
            // GET_LOCK 결과가 no-row/NULL/예상 밖 값이면 이 커넥션의 락 상태를 신뢰할 수 없으므로 폐기한다.
            logger.error("메타 덱 scheduler DB 락 획득 실패 - 커넥션을 폐기합니다. trigger={}", trigger, e);
            abortConnection(connection, trigger);
            return false;
        }

        if (!lockAcquired) {
            logger.info("메타 덱 집계 스킵 - 다른 인스턴스가 이미 scheduler DB 락을 보유 중. trigger={}", trigger);
            closeConnection(connection, trigger);
            return false;
        }

        try {
            task.run();
            return true;
        } finally {
            // MySQL named lock은 커넥션 단위라, 해제 실패/불명확 시 같은 물리 커넥션을 풀에 반환하면
            // 락이 남아있는 채로 재사용되어 다른 인스턴스가 계속 스킵되거나 재진입 카운트만 쌓일 수 있다.
            // 이 경우 커넥션을 abort()로 폐기해 물리 세션 자체를 없앤다.
            if (release(connection, trigger)) {
                closeConnection(connection, trigger);
            } else {
                abortConnection(connection, trigger);
            }
        }
    }

    private boolean tryAcquire(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, LOCK_NAME);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("GET_LOCK returned no result row");
                }
                int result = resultSet.getInt(1);
                if (resultSet.wasNull()) {
                    throw new SQLException("GET_LOCK returned NULL (unexpected DB error)");
                }
                if (result != 0 && result != 1) {
                    throw new SQLException("GET_LOCK returned unexpected value: " + result);
                }
                return result == 1;
            }
        }
    }

    private boolean release(Connection connection, String trigger) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    logger.warn("메타 덱 scheduler DB 락 RELEASE_LOCK 결과가 없습니다. trigger={}", trigger);
                    return false;
                }
                int result = resultSet.getInt(1);
                if (resultSet.wasNull() || result != 1) {
                    logger.warn(
                            "메타 덱 scheduler DB 락이 정상적으로 해제되지 않았습니다(result={}). trigger={}",
                            result, trigger
                    );
                    return false;
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("메타 덱 scheduler DB 락 해제 실패. trigger={}", trigger, e);
            return false;
        }
    }

    private void closeConnection(Connection connection, String trigger) {
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error("메타 덱 scheduler DB 커넥션 반납 실패. trigger={}", trigger, e);
        }
    }

    private void abortConnection(Connection connection, String trigger) {
        try {
            logger.warn("메타 덱 scheduler DB 락 상태가 불명확하여 커넥션을 폐기합니다. trigger={}", trigger);
            connection.abort(Runnable::run);
        } catch (SQLException e) {
            logger.error("메타 덱 scheduler DB 커넥션 폐기 실패. trigger={}", trigger, e);
        }
    }
}
