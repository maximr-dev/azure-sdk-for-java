// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.cosmos.implementation.Configs;
import org.testng.annotations.Test;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

public class HttpClientUtilsTest {
    private static final Configs configs = new Configs();
    private static final HttpClientConfig httpClientConfig = new HttpClientConfig(configs);

    @Test(groups = { "unit" })
    public void testCreateHttpClient() {
        HttpClient httpClient = null;
        try {
            httpClient = HttpClientUtils.createHttpClient(httpClientConfig);
            assertThat(httpClient.configuration().decoder().maxHeaderSize())
                .isEqualTo(configs.getMaxHttpHeaderSize());
            assertThat(httpClient.configuration().decoder().maxChunkSize())
                .isEqualTo(configs.getMaxHttpChunkSize());
            assertThat(httpClient.configuration().decoder().validateHeaders())
                .isTrue();
        } finally {
            close(httpClient);
        }
    }

    @Test(groups = { "unit" })
    public void testTestCreateHttpClient() {
        HttpClient httpClient = null;
        ConnectionProvider connectionProvider = ConnectionProvider
            .builder(configs.getReactorNettyConnectionPoolName())
            .maxConnections(33)
            .build();

        try {
            httpClient = HttpClientUtils.createHttpClient(httpClientConfig, connectionProvider);

            assertThat(httpClient.configuration().decoder().maxHeaderSize())
                .isEqualTo(configs.getMaxHttpHeaderSize());
            assertThat(httpClient.configuration().decoder().maxChunkSize())
                .isEqualTo(configs.getMaxHttpChunkSize());
            assertThat(httpClient.configuration().decoder().validateHeaders())
                .isTrue();
        } finally {
            close(httpClient);
        }
    }

    private void close(HttpClient httpClient) {
        if (httpClient != null) {
            httpClient.configuration().connectionProvider().dispose();
        }
    }
}
