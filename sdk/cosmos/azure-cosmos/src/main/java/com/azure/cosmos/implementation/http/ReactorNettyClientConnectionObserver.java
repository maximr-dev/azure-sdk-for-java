// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.http;

import java.time.Instant;
import java.util.function.BiConsumer;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientState;

class ReactorNettyClientConnectionObserver implements ConnectionObserver {
    static final String REACTOR_NETTY_REQUEST_RECORD_KEY = "reactorNettyRequestRecordKey";

    @Override
    public void onStateChange(Connection connection, State state) {
        Instant time = Instant.now();
        BiConsumer<ReactorNettyRequestRecord, Instant> timestampApplier = selectTimestampApplier(state);

        if (timestampApplier != null) {
            applyTimestampForRequestRecord(connection, state, timestampApplier, time);
        }
    }

    private void applyTimestampForRequestRecord(Connection connection, State state, BiConsumer<ReactorNettyRequestRecord, Instant> timestampApplier, Instant time) {
        if (state.equals(HttpClientState.CONNECTED) || state.equals(HttpClientState.ACQUIRED)) {
            if (connection instanceof ConnectionObserver) {
                ReactorNettyRequestRecord requestRecord = extractObservationRecord((ConnectionObserver) connection, state);
                timestampApplier.accept(requestRecord, time);
            }
        } else if (connection instanceof HttpClientRequest) {
            ReactorNettyRequestRecord requestRecord = extractObservationRecord((HttpClientRequest) connection, state);
            timestampApplier.accept(requestRecord, time);
        }
    }

    private ReactorNettyRequestRecord extractObservationRecord(ConnectionObserver connectionObserver, State state) {
        return (ReactorNettyRequestRecord) connectionObserver.currentContext()
            .getOrEmpty(REACTOR_NETTY_REQUEST_RECORD_KEY)
            .orElseThrow(() -> createIllegalStateException(state));
    }

    private ReactorNettyRequestRecord extractObservationRecord(HttpClientRequest httpClientRequest, State state) {
        return (ReactorNettyRequestRecord) httpClientRequest.currentContextView()
            .getOrEmpty(REACTOR_NETTY_REQUEST_RECORD_KEY)
            .orElseThrow(() -> createIllegalStateException(state));
    }

    private IllegalStateException createIllegalStateException(State state) {
        return new IllegalStateException("ReactorNettyRequestRecord not found in context for state: " + state);
    }

    private BiConsumer<ReactorNettyRequestRecord, Instant> selectTimestampApplier(State state) {
        if(HttpClientState.CONNECTED.equals(state) || HttpClientState.ACQUIRED.equals(state)) {
            return ReactorNettyRequestRecord::setTimeConnected;
        } else if(HttpClientState.CONFIGURED.equals(state)) {
            return ReactorNettyRequestRecord::setTimeConfigured;
        } else if(HttpClientState.REQUEST_SENT.equals(state)) {
            return ReactorNettyRequestRecord::setTimeSent;
        } else if(HttpClientState.RESPONSE_RECEIVED.equals(state)) {
            return ReactorNettyRequestRecord::setTimeReceived;
        }
        return null;
    }
}
