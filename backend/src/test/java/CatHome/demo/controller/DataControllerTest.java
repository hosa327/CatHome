package CatHome.demo.controller;

import CatHome.demo.service.MessageService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.OutputStream;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataController.class)
public class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    private MockHttpSession session;

    @BeforeEach
    public void setUp() {
        // 构造一个模拟用户 Session（如果你没有启用 Spring Security 可以跳过）
        User user = new User("testuser", "password",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextImpl securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities())
        );

        session = new MockHttpSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
    }

    @Test
    public void testExportTopicData_withValidSession_returnsCsvDownload() throws Exception {
        doNothing().when(messageService).writeFilteredTopicsAsCsv(Mockito.any(OutputStream.class),
                Mockito.eq(123L), Mockito.eq("test-topic"), Mockito.eq("mycat"));

        mockMvc.perform(get("/export/topic-data")
                        .param("userId", "123")
                        .param("topicName", "test-topic")
                        .param("catName", "mycat")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("topic-test-topic_mycat_user-123.csv")))
                .andExpect(header().string("Content-Type", Matchers.containsString("text/csv")));
    }

    @Test
    public void testExportTopicData_withoutSession_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/export/topic-data")
                        .param("userId", "123")
                        .param("topicName", "test-topic")
                        .param("catName", "mycat"))
                .andExpect(status().isUnauthorized());
    }
}
