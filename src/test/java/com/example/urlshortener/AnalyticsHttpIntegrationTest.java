package com.example.urlshortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsHttpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createAndGetCode(String url) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("code")
                .asText();
    }

    private long totalClicks(String code) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/urls/" + code + "/stats"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("totalClicks").asLong();
    }

    /** Click capture is async fire-and-forget, so stats assertions poll briefly. */
    private void awaitTotalClicks(String code, long expected) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        long observed = -1;
        while (Instant.now().isBefore(deadline)) {
            observed = totalClicks(code);
            if (observed == expected) {
                return;
            }
            Thread.sleep(100);
        }
        assertThat(observed).isEqualTo(expected);
    }

    @Test
    void redirectsAreCountedInStats() throws Exception {
        String code = createAndGetCode("https://example.com/analytics-target");

        mockMvc.perform(get("/" + code)
                        .header("Referer", "https://twitter.com/somepost")
                        .header("User-Agent", "IntegrationTest/1.0"))
                .andExpect(status().isFound());
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        awaitTotalClicks(code, 2);

        mockMvc.perform(get("/api/urls/" + code + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.totalClicks").value(2))
                .andExpect(jsonPath("$.clicksLast24h").value(2))
                .andExpect(jsonPath("$.lastClickAt").isNotEmpty());
    }

    @Test
    void statsForNeverClickedCodeAreZero() throws Exception {
        String code = createAndGetCode("https://example.com/never-clicked");

        mockMvc.perform(get("/api/urls/" + code + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(0))
                .andExpect(jsonPath("$.lastClickAt").doesNotExist());
    }

    @Test
    void statsForUnknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/api/urls/zzzzzz9/stats")).andExpect(status().isNotFound());
    }
}
