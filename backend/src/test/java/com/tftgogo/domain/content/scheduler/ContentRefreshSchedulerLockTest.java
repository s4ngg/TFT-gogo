package com.tftgogo.domain.content.scheduler;

import com.tftgogo.global.exception.BusinessException;
import com.tftgogo.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentRefreshSchedulerLockTest {

    private static final String LOCK_NAME = "tftgogo.content.refresh.scheduler.import";

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

    private ContentRefreshSchedulerLock schedulerLock;

    @BeforeEach
    void setUp() {
        schedulerLock = new ContentRefreshSchedulerLock(dataSource);
    }

    @Test
    void DB_락을_얻으면_task_실행후_같은_connection에서_락을_해제한다() throws Exception {
        // given
        givenLockAcquired();
        givenLockReleased();

        // when
        boolean acquired = schedulerLock.runWithLock("test", task);

        // then
        assertThat(acquired).isTrue();
        verify(acquireStatement).setString(1, LOCK_NAME);
        verify(acquireStatement).setInt(2, 0);
        verify(releaseStatement).setString(1, LOCK_NAME);
        InOrder inOrder = inOrder(connection, task);
        inOrder.verify(connection).prepareStatement("SELECT GET_LOCK(?, ?)");
        inOrder.verify(task).run();
        inOrder.verify(connection).prepareStatement("SELECT RELEASE_LOCK(?)");
        inOrder.verify(connection).close();
    }

    @Test
    void DB_락을_얻지_못하면_task와_release를_실행하지_않는다() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT GET_LOCK(?, ?)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResultSet);
        when(acquireResultSet.next()).thenReturn(true);
        when(acquireResultSet.getInt(1)).thenReturn(0);

        // when
        boolean acquired = schedulerLock.runWithLock("test", task);

        // then
        assertThat(acquired).isFalse();
        verify(task, never()).run();
        verify(connection, never()).prepareStatement("SELECT RELEASE_LOCK(?)");
        verify(connection, never()).abort(any(Executor.class));
        verify(connection).close();
    }

    @Test
    void DB_락_SQL_오류는_경쟁중으로_오인하지_않고_서버_오류로_전파한다() throws Exception {
        // given
        when(dataSource.getConnection()).thenThrow(new SQLException("database unavailable"));

        // when, then
        assertThatThrownBy(() -> schedulerLock.runWithLock("test", task))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
        verify(task, never()).run();
    }

    @Test
    void GET_LOCK_NULL은_connection을_abort하고_서버_오류로_전파한다() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT GET_LOCK(?, ?)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResultSet);
        when(acquireResultSet.next()).thenReturn(true);
        when(acquireResultSet.getInt(1)).thenReturn(0);
        when(acquireResultSet.wasNull()).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> schedulerLock.runWithLock("test", task))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));
        verify(connection).abort(any(Executor.class));
        verify(task, never()).run();
        verify(connection, never()).prepareStatement("SELECT RELEASE_LOCK(?)");
    }

    @Test
    void task에서_RuntimeException이_발생해도_락을_해제하고_예외를_전파한다() throws Exception {
        // given
        givenLockAcquired();
        givenLockReleased();
        doThrow(new RuntimeException("task failed")).when(task).run();

        // when, then
        assertThatThrownBy(() -> schedulerLock.runWithLock("test", task))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("task failed");
        verify(connection).prepareStatement("SELECT RELEASE_LOCK(?)");
        verify(connection).close();
    }

    @Test
    void 락_release가_실패하면_connection을_abort해_pool에_반환하지_않는다() throws Exception {
        // given
        givenLockAcquired();
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(releaseStatement);
        when(releaseStatement.executeQuery()).thenReturn(releaseResultSet);
        when(releaseResultSet.next()).thenReturn(true);
        when(releaseResultSet.getInt(1)).thenReturn(0);

        // when
        boolean acquired = schedulerLock.runWithLock("test", task);

        // then
        assertThat(acquired).isTrue();
        verify(connection).abort(any(Executor.class));
        verify(connection).close();
    }

    private void givenLockAcquired() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT GET_LOCK(?, ?)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResultSet);
        when(acquireResultSet.next()).thenReturn(true);
        when(acquireResultSet.getInt(1)).thenReturn(1);
    }

    private void givenLockReleased() throws Exception {
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(releaseStatement);
        when(releaseStatement.executeQuery()).thenReturn(releaseResultSet);
        when(releaseResultSet.next()).thenReturn(true);
        when(releaseResultSet.getInt(1)).thenReturn(1);
    }
}
