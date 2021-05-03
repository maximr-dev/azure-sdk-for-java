// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.http;

import static com.azure.cosmos.implementation.http.HttpClientConfig.REACTOR_NETWORK_LOG_CATEGORY;

import com.azure.core.http.ProxyOptions;
import com.azure.cosmos.implementation.Configs;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import io.netty.resolver.DefaultAddressResolverGroup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;

class HttpClientUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtils.class.getSimpleName());

    private static final MethodHandle HTTP_CLIENT_WARMUP;

    static {
        MethodHandle httpClientWarmup = null;
        try {
            httpClientWarmup = MethodHandles.publicLookup()
                .findVirtual(HttpClient.class, "warmup", MethodType.methodType(Mono.class));
        } catch (IllegalAccessException | NoSuchMethodException ex) {
            // Version of Reactor Netty doesn't have the warmup API on HttpClient.
            // So warmup won't be performed and this error is ignored.
        }

        HTTP_CLIENT_WARMUP = httpClientWarmup;
    }

    static HttpClient createHttpClient(HttpClientConfig config) {
        return createHttpClient(config, null);
    }

    static HttpClient createHttpClient(HttpClientConfig httpClientConfig, ConnectionProvider connectionProvider) {
        HttpClient httpClient = createClient(connectionProvider);
        httpClient = httpClient
            .observe(new ReactorNettyClientConnectionObserver())
            .resolver(DefaultAddressResolverGroup.INSTANCE);
        httpClient = configureProxy(httpClient, httpClientConfig.getProxy());
        httpClient = configureLogger(httpClient);

        Configs configs = httpClientConfig.getConfigs();
        return httpClient
            .secure(sslContextSpec -> sslContextSpec.sslContext(configs.getSslContext()))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) configs.getConnectionAcquireTimeout().toMillis())
            .httpResponseDecoder(httpResponseDecoderSpec ->
                httpResponseDecoderSpec.maxInitialLineLength(configs.getMaxHttpInitialLineLength())
                    .maxHeaderSize(configs.getMaxHttpHeaderSize())
                    .maxChunkSize(configs.getMaxHttpChunkSize())
                    .validateHeaders(true));
    }

    private static HttpClient createClient(ConnectionProvider connectionProvider) {
        return connectionProvider == null ? HttpClient.newConnection()
            : HttpClient.create(connectionProvider);
    }

    private static HttpClient configureProxy(HttpClient httpClient, ProxyOptions proxyOptions) {
        return proxyOptions == null ? httpClient
            : httpClient.proxy(typeSpec -> typeSpec.type(ProxyProvider.Proxy.HTTP).address(proxyOptions.getAddress()));
    }

    private static HttpClient configureLogger(HttpClient httpClient) {
        return LoggerFactory.getLogger(REACTOR_NETWORK_LOG_CATEGORY).isTraceEnabled()
            ? httpClient.wiretap(REACTOR_NETWORK_LOG_CATEGORY, LogLevel.INFO)
            : httpClient;
    }

    /*
     * This enables fast warm up of HttpClient
     */
    static void attemptToWarmupHttpClient(HttpClient httpClient) {
        // Warmup wasn't found, so don't attempt it.
        if (HTTP_CLIENT_WARMUP == null) {
            return;
        }

        try {
            ((Mono<?>) HTTP_CLIENT_WARMUP.invoke(httpClient)).block();
        } catch (ClassCastException | WrongMethodTypeException throwable) {
            // Invocation failed.
            logger.debug("Invoking HttpClient.warmup failed.", throwable);
        } catch (Throwable throwable) {
            // Warmup failed.
            throw new RuntimeException(throwable);
        }
    }
}
