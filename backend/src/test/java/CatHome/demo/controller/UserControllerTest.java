
package CatHome.demo.controller;

import CatHome.demo.dto.ApiResponse;
import CatHome.demo.dto.UserInfo;
import CatHome.demo.model.User;
import CatHome.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User("testuser", "test@example.com", "password123");
        mockUser.setAvatarRelativePath("testuser_avatar.png");
    }

    @Test
    void testRegister() throws Exception {
        Mockito.when(userService.register(any(), any(), any())).thenReturn(mockUser);

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testLogin() throws Exception {
        Mockito.when(userService.login(eq("test@example.com"), eq("password123"))).thenReturn(mockUser);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testCheckAvailability() throws Exception {
        Mockito.when(userService.checkAvailability("testuser", "test@example.com"))
                .thenReturn(Map.of("usernameExists", false, "emailExists", true));

        mockMvc.perform(post("/check-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usernameExists").value(false))
                .andExpect(jsonPath("$.emailExists").value(true));
    }

    @Test
    void testUploadAvatar() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "avatar".getBytes());
        UserInfo userInfo = new UserInfo(mockUser);

        Mockito.when(userService.uploadAvatar(eq(1L), any())).thenReturn(userInfo);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(multipart("/users/avatar").file(file).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarRelativePath").value("testuser_avatar.png"));
    }

    @Test
    void testGetUserInfo() throws Exception {
        Mockito.when(userService.getAvatarURL(1L)).thenReturn("http://example.com/avatar.png");
        Mockito.when(userService.getUserName(1L)).thenReturn("testuser");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(get("/userInfo")
                        .param("info", "avatar")
                        .param("info", "userName")
                        .param("info", "userId")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarURL").value("http://example.com/avatar.png"))
                .andExpect(jsonPath("$.userName").value("testuser"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void testChangePassword() throws Exception {
        Mockito.doNothing().when(userService).changePassword(1L, "oldPwd", "newPwd");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(post("/user/changePassword")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"oldPwd\",\"newPassword\":\"newPwd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully!"));
    }
}
