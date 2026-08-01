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

import com.example.demo.user.User;
import com.example.demo.user.UserRepository;
import com.example.demo.user.UserService;
import com.example.demo.user.dto.CreateUserRequest;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class BaseControllerTest {
    @Autowired
    protected RestTestClient restClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    protected final String baseUrl;

    protected static final String SESSION_NAME = "SESSION";

    private String getFormattedUri(String uri) {
        return "%s%s".formatted(baseUrl, uri);
    }

    public BaseControllerTest(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    protected User createUser(CreateUserRequest dto) {
        var response = userService.createUser(dto);
        return userRepository.findById(response.userId()).orElseThrow();
    }

    protected RestTestClient.RequestBodySpec post(String uri) {
        return restClient.post()
                .uri(getFormattedUri(uri))
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestBodySpec put(String uri) {
        return restClient.put()
                .uri(getFormattedUri(uri))
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestHeadersSpec<?> delete(String uri) {
        return restClient.delete()
                .uri(getFormattedUri(uri))
                .accept(MediaType.APPLICATION_JSON);
    }

    protected RestTestClient.RequestHeadersSpec<?> get(String uri) {
        return restClient.get()
                .uri(getFormattedUri(uri))
                .accept(MediaType.APPLICATION_JSON);
    }
}
