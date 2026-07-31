package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class BaseControllerTest {
    @Autowired
    protected RestTestClient restClient;

    protected static final String SESSION_NAME = "SESSION";

    protected RestTestClient.RequestBodySpec post(String uri) {
        return restClient.post()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestBodySpec put(String uri) {
        return restClient.put()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestHeadersSpec<?> delete(String uri) {
        return restClient.delete()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestHeadersSpec<?> get(String uri) {
        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON);
    }
}
