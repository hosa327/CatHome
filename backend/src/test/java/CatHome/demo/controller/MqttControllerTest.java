package CatHome.demo.controller;

import CatHome.demo.dto.ApiResponse;
import CatHome.demo.service.AwsIotService;
import CatHome.demo.service.MessageService;
import CatHome.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class MqttControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AwsIotService iotService;

    @MockBean
    private MessageService messageService;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        session.setAttribute("userId", 123L);
    }

    @Test
    void testAwsConnect_withSession_returnsSuccess() throws Exception {
        doNothing().when(iotService).initConnection(123L);

        mockMvc.perform(post("/mqtt/awsConnect").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Connection Successful"));
    }

    @Test
    void testAwsConnect_withoutSession_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/mqtt/awsConnect"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSubscribeTopic_withSession_success() throws Exception {
        doNothing().when(iotService).syncTopics(anyList(), eq(123L));

        mockMvc.perform(post("/mqtt/subscribeTopic")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"topic1\", \"topic2\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Subscribed to topics: [topic1, topic2]"));
    }

    @Test
    void testGetTopics_withSession_success() throws Exception {
        when(messageService.getTopics(123L)).thenReturn(List.of("topic1", "topic2"));

        mockMvc.perform(post("/mqtt/subscriptions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("topic1"));
    }

    @Test
    void testGetAWSstatus_withSession_success() throws Exception {
        when(iotService.checkStatus()).thenReturn(true);

        mockMvc.perform(get("/mqtt/awsStatus").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isConnected").value(true));
    }

    @Test
    void testUploadAWSinfo_success() throws Exception {
        MockMultipartFile cert = new MockMultipartFile("clientCert", "", "text/plain", "CERT_CONTENT".getBytes());
        MockMultipartFile key = new MockMultipartFile("clientKey", "", "text/plain", "KEY_CONTENT".getBytes());
        MockMultipartFile ca = new MockMultipartFile("caCert", "", "text/plain", "CA_CONTENT".getBytes());

        when(userService.uploadMqttInfo(eq(123L), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(null);
        doNothing().when(iotService).initConnection(123L);

        mockMvc.perform(multipart("/mqtt/config/upload")
                        .file(cert)
                        .file(key)
                        .file(ca)
                        .param("clientId", "client-123")
                        .param("endPoint", "endpoint.aws.com")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Connection Successful"));
    }
}
