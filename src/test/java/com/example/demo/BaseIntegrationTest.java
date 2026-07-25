package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class BaseIntegrationTest {

	@Autowired
	protected MockMvc mockMvc;

	protected final ObjectMapper objectMapper = new ObjectMapper();

	protected String toJson(Object dto) throws Exception {
		return objectMapper.writeValueAsString(dto);
	}

	protected ResultActions performPost(String url, Object body) throws Exception {
		return mockMvc.perform(post(url)
				.contentType(MediaType.APPLICATION_JSON)
				.content(toJson(body)));
	}
}
