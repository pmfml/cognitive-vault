package com.pmfml.cognitive_vault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

/**
 * Configuration class for Spring Data Elasticsearch.
 * Connects the application to the Elasticsearch cluster on port 9200.
 */
@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String uris;

    @Override
    public ClientConfiguration clientConfiguration() {
        // Remove protocol prefix if present, as connectedTo expects host:port (e.g. "localhost:9200")
        String hostAndPort = uris.replace("http://", "").replace("https://", "");

        return ClientConfiguration.builder()
                .connectedTo(hostAndPort)
                .build();
    }
}
