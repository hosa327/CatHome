package CatHome.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HomeKitDataPusherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private HomeKitDataPusher dataPusher;

    @BeforeEach
    void setUp() {
        // No additional setup required; @InjectMocks handles injection
    }

    @Test
    @DisplayName("pushLatestMessage(): should send message to /topic/{userId}/{catName}")
    void testPushLatestMessage() {
        Long userId = 42L;
        String catName = "Whiskers";
        String message = "Hello, cat!";

        dataPusher.pushLatestMessage(message, userId, catName);

        String expectedDestination = "/topic/" + userId + "/" + catName;
        verify(messagingTemplate, times(1))
                .convertAndSend(expectedDestination, message);
    }

    @Test
    @DisplayName("pushCatList(): should send cat list to /topic/{userId}/catList")
    void testPushCatList() {
        Long userId = 7L;
        List<String> catList = Arrays.asList("Mittens", "Shadow", "Tiger");

        dataPusher.pushCatList(userId, catList);

        String expectedDestination = "/topic/" + userId + "/catList";
        verify(messagingTemplate, times(1))
                .convertAndSend(expectedDestination, catList);
    }
}
