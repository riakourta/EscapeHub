package gr.ihu.escapehub.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SessionIntegrationTests {
    @Autowired
    private MockMvc mvc;

    @Test
    void fourDistinctLoginsRetainOwnProfilesAndRejectUnauthenticatedRequests() throws Exception {
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/ws/game")).andExpect(status().isUnauthorized());
        MockHttpSession[] sessions = new MockHttpSession[4];
        for (int i = 0; i < 4; i++) {
            String username = "integrationPlayer" + i;
            String credentials = "{\"username\":\"" + username + "\",\"password\":\"test-pass\"}";
            mvc.perform(post("/api/auth/register").with(csrf())
                    .contentType("application/json").content(credentials)).andExpect(status().isCreated());
            MvcResult login = mvc.perform(post("/api/auth/login").with(csrf())
                    .contentType("application/json").content(credentials))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString()).andReturn();
            sessions[i] = (MockHttpSession) login.getRequest().getSession(false);
            mvc.perform(get("/api/me").session(sessions[i]))
                    .andExpect(jsonPath("$.username").value(username));
            mvc.perform(put("/api/profile").session(sessions[i]).with(csrf())
                    .contentType("application/json")
                    .content("{\"nickname\":\"Robber" + i + "\",\"avatarColor\":\"Red\"}"))
                    .andExpect(status().isOk());
        }
        assertNotEquals(sessions[0].getId(), sessions[1].getId());
        for (int i = 0; i < 4; i++) {
            mvc.perform(get("/api/profile").session(sessions[i]))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.nickname").value("Robber" + i));
        }
        mvc.perform(put("/api/profile").session(sessions[0])
                .contentType("application/json")
                .content("{\"nickname\":\"Wrong\",\"avatarColor\":\"Red\"}"))
                .andExpect(status().isForbidden());
    }
}
