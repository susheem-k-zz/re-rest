package com.core.rerest.config;

import lombok.Data;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

@Data
@Configuration
@ConfigurationProperties(prefix = "rest.gateway.config")
public class RestConfig {

    private int connectTimeoutMillis;
    private int defaultMaxPerRoute;
    private int readTimeoutMillis;
    private int maxPoolTotal;

    @Bean
    public RestTemplate buildRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .requestFactory(buildClientHttpRequestFactory())
                .build();
    }

    private Supplier<ClientHttpRequestFactory> buildClientHttpRequestFactory() {
        final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMillis);
        requestFactory.setReadTimeout(readTimeoutMillis);
        requestFactory.setHttpClient(buildHttpClient());
        return () -> requestFactory;
    }

    private HttpClient buildHttpClient() {
        return HttpClients.custom()
                .setConnectionManager(buildHttpConnectionManager())
                .build();
    }

    private HttpClientConnectionManager buildHttpConnectionManager() {
        final PoolingHttpClientConnectionManager clientConnectionManager = new PoolingHttpClientConnectionManager();
        clientConnectionManager.setMaxTotal(maxPoolTotal);
        clientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        return clientConnectionManager;
    }

}
