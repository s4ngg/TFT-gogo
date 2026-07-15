package com.tftgogo.domain.deck.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaDeckSchedulerLockTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement acquireStatement;

    @Mock
    private PreparedStatement releaseStatement;

    @Mock
    private ResultSet acquireResultSet;

    @Mock
    private ResultSet releaseResultSet;

    @Mock
    private Runnable task;

    private MetaDeckSchedulerLock schedulerLock;

    @BeforeEach
    void setUp() throws Exception {
        schedulerLock = new MetaDeckSchedulerLock(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
    }

    @Test
    void DB_락을_얻으면_task_실행후_같은_커넥션에서_락을_해제하고_풀에_반환한다() throws Exception {
        // given
        givenAcquireReturns(1, false);
        givenReleaseReturns(1, false);

        // when
        boolean acquired = schedulerLock.runWithLock("test", task);

        // then
        assertThat(acquired).isTrue();
        verify(task).run();
        verify(connection).close();
        verify(connection, never()).abort(any(Executor.class));
    }

    @Test
    void DB_락을_이미_다른_인스턴스가_보유중이면_task를_실행하지_않고_커넥션은_정상_반환한다() throws Exception {
        // given
        givenAcquireReturns(0, false);

        // when
        boolean acquired = schedulerLock.runWithLock("test", task);

        // then
        assertThat(acquired).isFalse();
        verify(task, never()).run();
        verify(connection).close();
        verify(connection, never()).abort(any(Executor.class));
    }

    @Test
    void GET_LOCK이_NULL을_반환하면_task를_실행하지_않고_커넥션을_abort한다() throws Exception {
        // given
        givenAcquireReturns(0, true);

        // when
        boolean acquired = schedulerLock.runWithLock("test", task);

        // then
        assertThat(acquired).isFalse();
        verify(task, never()).run();
        verify(connection, never()).close();
        verify(connection).abort(any(Executor.class));
    }

    @Test
    void GET_LOCK_결과가_no_row이면_task를_실행하지_않고_커넥션을_abort한다() throws Exception {
        // given
        when(connection.prepareStatement("SELECT GET_LOCK(?, ?)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResultSet);
        when(acquireResultSet.next()).thenReturn(false);

        // when
        boolean acquired = schedulerLock.runWithLock("test", task);

        // then
        assertThat(acquired).isFalse();
        verify(task, never()).run();
        verify(connection, never()).close();
        verify(connection).abort(any(Executor.class));
    }

    @Test
    void RELEASE_LOCK이_0을_반환하면_task는_실행되지만_커넥션은_abort한다() throws Exception {
        // given
        givenAcquireReturns(1, false);
        givenReleaseReturns(0, false);

        // when
        boolean acquired = schedulerLock.runWithLock("test", task);

        // then
        assertThat(acquired).isTrue();
        verify(task).run();
        verify(connection, never()).close();
        verify(connection).abort(any(Executor.class));
    }

    @Test
    void RELEASE_LOCK이_예외를_던지면_커넥션을_abort한다() throws Exception {
        // given
        givenAcquireReturns(1, false);
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(releaseStatement);
        when(releaseStatement.executeQuery()).thenThrow(new SQLException("connection reset"));

        // when
        boolean acquired = schedulerLock.runWithLock("test", task);

        // then
        assertThat(acquired).isTrue();
        verify(task).run();
        verify(connection, never()).close();
        verify(connection).abort(any(Executor.class));
    }

    private void givenAcquireReturns(int value, boolean wasNull) throws Exception {
        when(connection.prepareStatement("SELECT GET_LOCK(?, ?)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResultSet);
        when(acquireResultSet.next()).thenReturn(true);
        when(acquireResultSet.getInt(1)).thenReturn(value);
        when(acquireResultSet.wasNull()).thenReturn(wasNull);
    }

    private void givenReleaseReturns(int value, boolean wasNull) throws Exception {
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(releaseStatement);
        when(releaseStatement.executeQuery()).thenReturn(releaseResultSet);
        when(releaseResultSet.next()).thenReturn(true);
        when(releaseResultSet.getInt(1)).thenReturn(value);
        when(releaseResultSet.wasNull()).thenReturn(wasNull);
    }
}
