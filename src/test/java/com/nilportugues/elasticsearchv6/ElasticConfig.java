package com.nilportugues.elasticsearchv6;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticConfig {
    public static final RestHighLevelClient CLIENT = new RestHighLevelClient(
        RestClient
                .builder(new HttpHost("localhost", 9200, "http"), new HttpHost("localhost", 9201, "http"))
                .setMaxRetryTimeoutMillis(100)
    );

    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static final BasicHeader JSON_HEADER = new BasicHeader("Content-type", "application/json");
}
