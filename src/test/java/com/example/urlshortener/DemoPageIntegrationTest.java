package com.example.urlshortener;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The demo page is a static shell over the API; it must be served at the root
 * without shadowing the /{code} redirect mapping (code pattern requires 1-16
 * alphanumerics, so "/" and "index.html" fall through to static resources).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DemoPageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootForwardsToTheDemoPage() throws Exception {
        // Spring Boot's welcome-page mapping forwards "/" to index.html;
        // MockMvc surfaces the forward rather than following it.
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl("index.html"));
    }

    @Test
    void demoPageIsServedAsHtml() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Shrink Ray")));
    }

    @Test
    void demoPageDoesNotShadowRedirectRoutes() throws Exception {
        // A well-formed but unknown code must still reach the redirect handler (404),
        // not fall back to the static page (which would be a 200).
        mockMvc.perform(get("/zzzzzz9")).andExpect(status().isNotFound());
    }
}
