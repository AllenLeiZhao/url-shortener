package com.example.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class EdgeCaseHttpIntegrationTest {

    private static final int CONCURRENT_CREATES = 30;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void concurrentCreatesAllSucceedWithUniqueCodes() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_CREATES; i++) {
                final int n = i;
                tasks.add(() -> {
                    MvcResult result = mockMvc.perform(post("/api/urls")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"url\":\"https://example.com/concurrent/" + n + "\"}"))
                            .andExpect(status().isCreated())
                            .andReturn();
                    return objectMapper
                            .readTree(result.getResponse().getContentAsString())
                            .get("code")
                            .asText();
                });
            }
            List<Future<String>> futures = pool.invokeAll(tasks);
            Set<String> codes = futures.stream()
                    .map(f -> {
                        try {
                            return f.get();
                        } catch (Exception e) {
                            throw new AssertionError("concurrent create failed", e);
                        }
                    })
                    .collect(Collectors.toSet());

            assertThat(codes).hasSize(CONCURRENT_CREATES);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void sameUrlShortenedTwiceYieldsDistinctCodes() throws Exception {
        // Documented ADR-002 trade-off: no dedupe by design
        String body = "{\"url\":\"https://example.com/duplicate-me\"}";
        MvcResult r1 = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult r2 = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String code1 = objectMapper
                .readTree(r1.getResponse().getContentAsString())
                .get("code")
                .asText();
        String code2 = objectMapper
                .readTree(r2.getResponse().getContentAsString())
                .get("code")
                .asText();
        assertThat(code1).isNotEqualTo(code2);
    }

    @Test
    void codesOutsideTheAllowedShapeDoNotHitTheRedirectHandler() throws Exception {
        // Redirect mapping is constrained to [0-9A-Za-z]{1,16}; anything else must not resolve
        mockMvc.perform(get("/abc!def")).andExpect(status().isNotFound());
        mockMvc.perform(get("/" + "a".repeat(17))).andExpect(status().isNotFound());
    }

    @Test
    void malformedJsonBodyReturns400WithStructuredError() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
    }
}
