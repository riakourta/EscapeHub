package gr.ihu.escapehub.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.ihu.escapehub.auth.entity.User;
import gr.ihu.escapehub.profile.controller.ProfileController;
import gr.ihu.escapehub.profile.dto.UpdateProfileDto;
import gr.ihu.escapehub.profile.entity.Profile;
import gr.ihu.escapehub.profile.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
public class ProfileTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProfileService profileService;

    @Test
    public void shouldRejectUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldUpdateProfileSuccessfully() throws Exception {
        User principalUser = new User("professor", "hashed");
        principalUser.setId(1L);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principalUser,
                null,
                List.of()
        );

        UpdateProfileDto request = new UpdateProfileDto();
        request.setNickname("ElProfessor");
        request.setAvatarColor("Red");

        Profile updatedProfile = new Profile();
        updatedProfile.setId(1L);
        updatedProfile.setNickname("ElProfessor");
        updatedProfile.setAvatarColor("Red");

        Mockito.when(profileService.updateProfile(Mockito.eq(1L), Mockito.any(UpdateProfileDto.class)))
                .thenReturn(updatedProfile);

        mockMvc.perform(put("/api/profile")
                        .with(authentication(auth))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("ElProfessor"))
                .andExpect(jsonPath("$.avatarColor").value("Red"));
    }
}