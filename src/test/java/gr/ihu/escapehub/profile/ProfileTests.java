package gr.ihu.escapehub.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.profile.controller.ProfileController;
import gr.ihu.escapehub.profile.dto.UpdateProfileDto;
import gr.ihu.escapehub.profile.entity.Profile;
import gr.ihu.escapehub.profile.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
public class ProfileTests {

    @Autowired
    private MockMvc mockMvc;

    // Εργαλείο για να μετατρέπουμε αντικείμενα σε JSON
    @Autowired
    private ObjectMapper objectMapper;

    // Εικονικό Service (mock) αφού δεν έχουμε βάση δεδομένων συνδεδεμένη στο τεστ
    @MockBean
    private ProfileService profileService;

    // ΤΕΣΤ 1: Απορρίπτει τα αιτήματα χωρίς σύνδεση (Ολοκληρώθηκε)
    @Test
    public void shouldRejectUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    // ΤΕΣΤ 2: Επιβεβαιώνει την ανανέωση του προφίλ
    @Test
    // @WithMockUser // Ίσως χρειαστεί να βγάλεις το σχόλιο αργότερα αν το Άτομο 1 ενεργοποιήσει το Security
    public void shouldUpdateProfileSuccessfully() throws Exception {

        // 1. Προετοιμάζουμε τα δεδομένα που στέλνει υποθετικά ο χρήστης
        UpdateProfileDto request = new UpdateProfileDto();
        request.setNickname("ElProfessor");
        request.setAvatarColor("Red");

        // 2. Προετοιμάζουμε την εικονική απάντηση της βάσης
        Profile updatedProfile = new Profile();
        updatedProfile.setId(1L);
        updatedProfile.setNickname("ElProfessor");
        updatedProfile.setAvatarColor("Red");

        // Λέμε στο εικονικό Service: "Όταν σου ζητήσουν update, επέστρεψε το updatedProfile"
        Mockito.when(profileService.updateProfile(Mockito.anyLong(), Mockito.any(UpdateProfileDto.class)))
                .thenReturn(updatedProfile);

        // 3. Εκτελούμε το PUT αίτημα με τα δεδομένα σε μορφή JSON
        mockMvc.perform(put("/api/profile")
                        // .with(csrf()) // Το ίδιο, μπορεί να χρειαστεί όταν το Άτομο 1 ενώσει το Security
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 4. Ελέγχουμε αν το σύστημα απαντάει σωστά με τα νέα δεδομένα
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("ElProfessor"))
                .andExpect(jsonPath("$.avatarColor").value("Red"));
    }
}