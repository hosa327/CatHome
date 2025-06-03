package CatHome.demo.service;

import CatHome.demo.exception.ConnectionException;
import CatHome.demo.repository.TopicMessageRepository;
import CatHome.demo.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsIotServiceTest {

    @Mock
    private AwsConnectionFactory connFactory;

    @Mock
    private MessageService messageService;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private TopicMessageRepository topicMessageRepository;

    @Mock
    private HomeKitDataPusher pusher;

    @Mock
    private MqttClientConnection mockConnection;

    private AwsIotService awsIotService;

    @BeforeEach
    void setUp() throws Exception {
        awsIotService = new AwsIotService(
                connFactory,
                messageService,
                topicRepository,
                topicMessageRepository,
                pusher
        );
        // Mark these stubbings lenient to avoid UnnecessaryStubbingException in tests that don't need them
        lenient().when(connFactory.createConnection(anyLong()))
                .thenReturn(mockConnection);
        lenient().when(mockConnection.unsubscribe(anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        lenient().when(mockConnection.subscribe(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    @DisplayName("initConnection(): sets connection and checkStatus returns true")
    void testInitConnection_SetsConnection() throws Exception {
        assertThat(awsIotService.checkStatus()).isFalse();

        awsIotService.initConnection(123L);
        verify(connFactory, times(1)).createConnection(123L);
        assertThat(awsIotService.checkStatus()).isTrue();
    }

    @Test
    @DisplayName("checkStatus(): false before init, true after init")
    void testCheckStatus() throws Exception {
        assertThat(awsIotService.checkStatus()).isFalse();

        awsIotService.initConnection(456L);
        assertThat(awsIotService.checkStatus()).isTrue();
    }

    @Test
    @DisplayName("syncTopics(): throws ConnectionException if connection is null")
    void testSyncTopics_NoConnection_Throws() {
        List<String> newTopics = Arrays.asList("A", "B");
        assertThatThrownBy(() -> awsIotService.syncTopics(newTopics, 1L))
                .isInstanceOf(ConnectionException.class)
                .hasMessage("Failed to connect to AWS IoT Core");
    }

    @Test
    @DisplayName("syncTopics(): unsubscribes removed topics and subscribes new ones")
    void testSyncTopics_ExistingOldTopics() throws Exception {
        Long userId = 10L;
        List<String> newTopics = Arrays.asList("topic1", "topic2");

        awsIotService.initConnection(userId);
        when(topicRepository.findTopicNamesByUserId(userId))
                .thenReturn(Optional.of(Arrays.asList("topic1", "oldTopic")));

        awsIotService.syncTopics(newTopics, userId);

        verify(messageService, times(1)).deleteTopic(userId, "oldTopic");
        verify(messageService, times(1)).addTopic(userId, "topic1");
        verify(messageService, times(1)).addTopic(userId, "topic2");

        verify(mockConnection, times(1)).unsubscribe("oldTopic");
        verify(mockConnection, times(1)).subscribe(eq("topic1"), any(), any());
        verify(mockConnection, times(1)).subscribe(eq("topic2"), any(), any());
    }

    @Test
    @DisplayName("syncTopics(): no old topics results in unsubscribing and subscribing all new topics")
    void testSyncTopics_NoOldTopics() throws Exception {
        Long userId = 20L;
        List<String> newTopics = Arrays.asList("X", "Y");

        awsIotService.initConnection(userId);
        when(topicRepository.findTopicNamesByUserId(userId))
                .thenReturn(Optional.empty());

        awsIotService.syncTopics(newTopics, userId);

        verify(messageService, times(1)).deleteTopic(userId, "X");
        verify(messageService, times(1)).deleteTopic(userId, "Y");
        verify(messageService, times(1)).addTopic(userId, "X");
        verify(messageService, times(1)).addTopic(userId, "Y");

        verify(mockConnection, times(1)).unsubscribe("X");
        verify(mockConnection, times(1)).unsubscribe("Y");
        verify(mockConnection, times(1)).subscribe(eq("X"), any(), any());
        verify(mockConnection, times(1)).subscribe(eq("Y"), any(), any());
    }

    @Test
    @DisplayName("syncTopics(): when new and old topics match, still calls addTopic for each new topic")
    void testSyncTopics_SameTopics_AddsAllNewTopics() throws Exception {
        Long userId = 30L;
        List<String> newTopics = Arrays.asList("tA", "tB");

        awsIotService.initConnection(userId);
        when(topicRepository.findTopicNamesByUserId(userId))
                .thenReturn(Optional.of(Arrays.asList("tA", "tB")));

        awsIotService.syncTopics(newTopics, userId);

        verify(messageService, never()).deleteTopic(anyLong(), anyString());
        verify(messageService, times(1)).addTopic(userId, "tA");
        verify(messageService, times(1)).addTopic(userId, "tB");

        verify(mockConnection, never()).unsubscribe(anyString());
        verify(mockConnection, times(1)).subscribe(eq("tA"), any(), any());
        verify(mockConnection, times(1)).subscribe(eq("tB"), any(), any());
    }
}
