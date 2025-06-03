package CatHome.demo.controller;

import CatHome.demo.model.LatestDataMessage;
import CatHome.demo.repository.LatestDataMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class WebSocketControllerTest {

    private SimpMessagingTemplate messagingTemplate;
    private LatestDataMessageRepository messageRepository;
    private WebSocketController controller;

    @BeforeEach
    public void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        messageRepository = mock(LatestDataMessageRepository.class);
        controller = new WebSocketController(messagingTemplate, messageRepository);
    }

    @Test
    public void testRequestCatList() {
        Long userId = 123L;
        List<String> catList = List.of("Luna", "Oscar");

        // Mock repository behavior
        when(messageRepository.findCatNamesByUserId(userId)).thenReturn(catList);

        // Build payload and invoke
        Map<String, String> payload = Map.of("userId", userId.toString());
        controller.onRequestCatList(payload);

        // Verify message was sent to the correct topic
        verify(messagingTemplate).convertAndSend("/topic/" + userId + "/catList", catList);
    }

    @Test
    public void testRequestLatest() {
        Long userId = 456L;
        String catName = "Fluffy";
        String expectedPayload = "{\"temp\": 23}";

        LatestDataMessage msg = mock(LatestDataMessage.class);
        when(msg.getPayload()).thenReturn(expectedPayload);

        when(messageRepository.findByUserIdAndCatName(userId, catName))
                .thenReturn(Optional.of(msg));

        Map<String, String> payload = Map.of(
                "userId", userId.toString(),
                "catName", catName
        );

        controller.onRequestLatest(payload);

        verify(messagingTemplate).convertAndSend("/topic/" + userId + "/" + catName, expectedPayload);
    }
}
