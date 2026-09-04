package com.example.urlshortener;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Runs in its own context with a low create limit so the general suite is unaffected.
 */
@SpringBootTest(properties = "app.rate-limit.create-per-minute=3")
@AutoConfigureMockMvc
class ReliabilityHttpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createIsRateLimitedPerIpButRedirectsAreNot() throws Exception {
        String code = null;
        for (int i = 1; i <= 3; i++) {
            MvcResult result = mockMvc.perform(post("/api/urls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"url\":\"https://example.com/rl-" + i + "\"}"))
                    .andExpect(status().isCreated())
                    .andReturn();
            code = objectMapper
                    .readTree(result.getResponse().getContentAsString())
                    .get("code")
                    .asText();
        }

        // 4th create in the window → 429 with Retry-After
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/rl-4\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.status").value(429));

        // Read path must be unaffected by write-path limiting
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());
        mockMvc.perform(get("/api/urls/" + code)).andExpect(status().isOk());
    }

    @Test
    void healthEndpointReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }
}
