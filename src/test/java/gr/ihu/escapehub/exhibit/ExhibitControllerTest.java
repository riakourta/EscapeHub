package gr.ihu.escapehub.exhibit;

import gr.ihu.escapehub.exhibit.controller.ExhibitController;
import gr.ihu.escapehub.exhibit.service.ExhibitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExhibitControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ExhibitService exhibitService = new ExhibitService();
        ExhibitController controller = new ExhibitController(exhibitService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllExhibitsReturnsOkAndFiveExhibits() throws Exception {
        mockMvc.perform(get("/api/exhibits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void getExhibitByIdReturnsExhibitDetails() throws Exception {
        mockMvc.perform(get("/api/exhibits/exhibit-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("exhibit-2"))
                .andExpect(jsonPath("$.name").isNotEmpty())
                .andExpect(jsonPath("$.clues").isArray());
    }

    @Test
    void getUnknownExhibitReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/exhibits/unknown"))
                .andExpect(status().isNotFound());
    }
}