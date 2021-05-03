// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

import java.util.Optional;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientState;

public class ReactorNettyClientConnectionObserverTest {

    private AutoCloseable openMocks;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS, extraInterfaces = {Connection.class})
    private ConnectionObserver connectionObserver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS, extraInterfaces = {Connection.class})
    private HttpClientRequest httpClientRequest;

    private final ReactorNettyClientConnectionObserver underTest = new ReactorNettyClientConnectionObserver();

    @BeforeMethod(alwaysRun = true)
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);
    }

    @AfterMethod(alwaysRun = true)
    public void after() throws Exception {
        openMocks.close();
        Mockito.framework().clearInlineMocks();
    }

    @Test(groups = {"unit"})
    public void testIllegalStateForAcquired() {
        when(connectionObserver.currentContext().getOrEmpty(any()))
            .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
            () -> underTest.onStateChange((Connection) connectionObserver, HttpClientState.ACQUIRED));
    }

    @Test(groups = {"unit"})
    public void testIllegalStateForSent() {
        when(httpClientRequest.currentContextView().getOrEmpty(any()))
            .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
            () -> underTest.onStateChange((Connection) httpClientRequest, HttpClientState.REQUEST_SENT));
    }

    @Test(groups = {"unit"})
    public void testConnected() {
        ReactorNettyRequestRecord record = mock(ReactorNettyRequestRecord.class);
        when(connectionObserver.currentContext().getOrEmpty(any()))
            .thenReturn(Optional.of(record));

        underTest.onStateChange((Connection) connectionObserver, HttpClientState.CONNECTED);

        verify(record, times(1)).setTimeConnected(any());
    }

    @Test(groups = {"unit"})
    public void testAcquired() {
        ReactorNettyRequestRecord record = mock(ReactorNettyRequestRecord.class);
        when(connectionObserver.currentContext().getOrEmpty(any()))
            .thenReturn(Optional.of(record));

        underTest.onStateChange((Connection) connectionObserver, HttpClientState.ACQUIRED);

        verify(record, times(1)).setTimeConnected(any());
    }

    @Test(groups = {"unit"})
    public void testRequestSent() {
        ReactorNettyRequestRecord record = mock(ReactorNettyRequestRecord.class);
        when(httpClientRequest.currentContextView().getOrEmpty(any()))
            .thenReturn(Optional.of(record));

        underTest.onStateChange((Connection) httpClientRequest, HttpClientState.REQUEST_SENT);

        verify(record, times(1)).setTimeSent(any());
    }

    @Test(groups = {"unit"})
    public void testRequestReceived() {
        ReactorNettyRequestRecord record = mock(ReactorNettyRequestRecord.class);
        when(httpClientRequest.currentContextView().getOrEmpty(any()))
            .thenReturn(Optional.of(record));

        underTest.onStateChange((Connection) httpClientRequest, HttpClientState.RESPONSE_RECEIVED);

        verify(record, times(1)).setTimeReceived(any());
    }

    @Test(groups = {"unit"})
    public void testConfigured() {
        ReactorNettyRequestRecord record = mock(ReactorNettyRequestRecord.class);
        when(httpClientRequest.currentContextView().getOrEmpty(any()))
            .thenReturn(Optional.of(record));

        underTest.onStateChange((Connection) httpClientRequest, HttpClientState.CONFIGURED);

        verify(record, times(1)).setTimeConfigured(any());
    }

    @Test(groups = {"unit"})
    public void testUnmappedState() {
        underTest.onStateChange((Connection) httpClientRequest, HttpClientState.RELEASED);

        verify(httpClientRequest, times(0)).currentContextView();
    }
}
