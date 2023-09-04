package com.caovy2001.chatbot.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.util.ResourceBundle;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.caovy2001.chatbot.repository.es")
@ComponentScan(basePackages = { "com.caovy2001.chatbot" })
public class ElasticSearchConfig extends AbstractElasticsearchConfiguration {
    private final ResourceBundle resourceBundle = ResourceBundle.getBundle("application");

    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {

        final ClientConfiguration clientConfiguration =
                ClientConfiguration
                        .builder()
                        .connectedTo(resourceBundle.getString("elastic_search.server"))
                        .build();

        return RestClients.create(clientConfiguration).rest();
    }
}