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

import static org.assertj.core.api.Assertions.assertThat;
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
    void setUp() {
        schedulerLock = new MetaDeckSchedulerLock(dataSource);
    }

    @Test
    void DB_락을_얻으면_task_실행후_같은_커넥션에서_락을_해제한다() throws Exception {
        // given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT GET_LOCK(?, ?)")).thenReturn(acquireStatement);
        when(acquireStatement.executeQuery()).thenReturn(acquireResultSet);
        when(acquireResultSet.next()).thenReturn(true);
        when(acquireResultSet.getInt(1)).thenReturn(1);
        when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenReturn(releaseStatement);
        when(releaseStatement.executeQuery()).thenReturn(releaseResultSet);
        when(releaseResultSet.next()).thenReturn(true);
        when(releaseResultSet.getInt(1)).thenReturn(1);

        // when
        boolean acquired = schedulerLock.runWithLock("test", task);

        // then
        assertThat(acquired).isTrue();
        verify(task).run();
        verify(connection).prepareStatement("SELECT GET_LOCK(?, ?)");
        verify(connection).prepareStatement("SELECT RELEASE_LOCK(?)");
        verify(connection).close();
    }

    @Test
    void DB_락을_얻지_못하면_task를_실행하지_않는다() throws Exception {
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
        verify(connection).close();
    }
}
