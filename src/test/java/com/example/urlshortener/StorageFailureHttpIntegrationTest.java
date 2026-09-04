package com.example.urlshortener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Reliability R3 (graceful degradation): storage-layer failures must surface as a
 * clean 503 with the structured error body, never a stack-trace 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StorageFailureHttpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShortUrlRepository repository;

    @Test
    void lookupDuringStorageOutageReturns503() throws Exception {
        when(repository.findByCode(anyString()))
                .thenThrow(new DataAccessResourceFailureException("connection refused"));

        mockMvc.perform(get("/api/urls/Abc1234"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Storage temporarily unavailable, please retry"));
    }

    @Test
    void createDuringStorageOutageReturns503() throws Exception {
        when(repository.save(any())).thenThrow(new DataAccessResourceFailureException("connection refused"));

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/during-outage\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }
}
