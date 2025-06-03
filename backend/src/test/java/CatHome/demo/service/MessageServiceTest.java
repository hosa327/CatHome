package CatHome.demo.service;

import CatHome.demo.exception.TopicException;
import CatHome.demo.model.*;
import CatHome.demo.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private UserMessageRepository userMessageRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private LatestDataMessageRepository latestDataMessageRepository;

    @Mock
    private TopicMessageRepository topicMessageRepository;

    @Mock
    private HomeKitDataPusher pusher;

    private MessageService messageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                userMessageRepository,
                topicRepository,
                latestDataMessageRepository,
                objectMapper,
                topicMessageRepository,
                pusher
        );
    }

    @Test
    @DisplayName("separateMessage: valid JSON splits into catName and payload")
    void testSeparateMessage_Success() {
        String raw = "{\"catName\":\"Felix\",\"temperature\":36.5}";
        Map<String, String> result = messageService.separateMessage(raw);

        assertThat(result.get("catName")).isEqualTo("Felix");
        assertThat(result.get("payload")).contains("\"temperature\":36.5");
    }

    @Test
    @DisplayName("separateMessage: invalid JSON throws TopicException")
    void testSeparateMessage_Failure() {
        String raw = "not a json";
        assertThatThrownBy(() -> messageService.separateMessage(raw))
                .isInstanceOf(TopicException.class)
                .hasMessageContaining("Parsing message failed");
    }

    @Test
    @DisplayName("addTopic: new user creates UserMessages and adds Topic")
    void testAddTopic_NewUser() {
        Long uid = 1L;
        String topicName = "weather";

        when(userMessageRepository.findById(uid)).thenReturn(Optional.empty());
        when(userMessageRepository.save(any(UserMessages.class))).thenAnswer(inv -> inv.getArgument(0));

        UserMessages result = messageService.addTopic(uid, topicName);

        assertThat(result).isNotNull();
        assertThat(result.getTopics())
                .extracting(Topic::getTopicName)
                .containsExactly(topicName);

        verify(userMessageRepository, times(2)).save(any(UserMessages.class));
    }

    @Test
    @DisplayName("addTopic: existing user with no such topic adds it")
    void testAddTopic_ExistingUserNewTopic() {
        Long uid = 2L;
        String topicName = "news";

        UserMessages existing = new UserMessages(uid);
        when(userMessageRepository.findById(uid)).thenReturn(Optional.of(existing));
        when(topicRepository.findTopicByUserIdAndTopicName(uid, topicName))
                .thenReturn(Optional.empty());
        when(userMessageRepository.save(existing)).thenReturn(existing);

        UserMessages result = messageService.addTopic(uid, topicName);

        assertThat(result.getTopics())
                .extracting(Topic::getTopicName)
                .containsExactly(topicName);
        verify(userMessageRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("deleteTopic: existing user and topic removes it")
    void testDeleteTopic_ExistingUser() {
        Long uid = 3L;
        String topicName = "sports";

        UserMessages existing = new UserMessages(uid);
        Topic t = new Topic(topicName);
        existing.addTopic(t);

        when(userMessageRepository.findById(uid)).thenReturn(Optional.of(existing));
        when(topicRepository.findTopicByUserIdAndTopicName(uid, topicName))
                .thenReturn(Optional.of(t));
        when(userMessageRepository.save(existing)).thenReturn(existing);

        UserMessages result = messageService.deleteTopic(uid, topicName);

        assertThat(result.getTopics()).isEmpty();
        verify(userMessageRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("getTopics: when no topics exist, returns empty list")
    void testGetTopics_Empty() {
        Long uid = 4L;
        when(topicRepository.findTopicNamesByUserId(uid)).thenReturn(Optional.empty());

        List<String> list = messageService.getTopics(uid);
        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("saveTopicMessage: missing topic throws TopicException")
    void testSaveTopicMessage_TopicNotFound() {
        Long uid = 5L;
        String topicName = "fun";
        String message = "{\"catName\":\"Tom\",\"score\":100}";

        when(userMessageRepository.findById(uid)).thenReturn(Optional.empty());
        when(topicRepository.findTopicByUserIdAndTopicName(uid, topicName)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.saveTopicMessage(uid, topicName, message))
                .isInstanceOf(TopicException.class)
                .hasMessage("Topic Not Found");
    }

    @Test
    @DisplayName("saveTopicMessage: existing topic saves and returns it")
    void testSaveTopicMessage_Success() {
        Long uid = 6L;
        String topicName = "tech";
        String raw = "{\"catName\":\"Luna\",\"value\":42}";

        UserMessages user = new UserMessages(uid);
        when(userMessageRepository.findById(uid)).thenReturn(Optional.of(user));
        Topic topic = new Topic(topicName);
        when(topicRepository.findTopicByUserIdAndTopicName(uid, topicName))
                .thenReturn(Optional.of(topic));
        when(topicRepository.save(topic)).thenReturn(topic);

        Topic result = messageService.saveTopicMessage(uid, topicName, raw);

        assertThat(result).isSameAs(topic);
        assertThat(topic.getMessages()).hasSize(1);
        TopicMessage tm = topic.getMessages().get(0);
        assertThat(tm.getCatName()).isEqualTo("Luna");
        assertThat(tm.getPayload()).contains("\"value\":42");
    }

    @Test
    @DisplayName("writeFilteredTopicsAsCsv: outputs correct CSV format")
    void testWriteFilteredTopicsAsCsv() throws Exception {
        Long uid = 7L;
        String topicName = "pets";
        String catName = "Bella";

        LocalDateTime now = LocalDateTime.of(2025, 6, 3, 12, 0, 0);
        TopicMessage tm1 = new TopicMessage(catName, "payload1", now);
        TopicMessage tm2 = new TopicMessage(catName, "payload,with,comma", now);

        when(topicMessageRepository.findByUserIdAndTopicNameAndCatName(uid, topicName, catName))
                .thenReturn(Arrays.asList(tm1, tm2));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        messageService.writeFilteredTopicsAsCsv(baos, uid, topicName, catName);

        String csv = baos.toString(StandardCharsets.UTF_8);
        assertThat(csv).startsWith("catName,receivedAt,payload\n");
        assertThat(csv).contains("Bella,2025-06-03 12:00:00,payload1");
        assertThat(csv).contains("Bella,2025-06-03 12:00:00,\"payload,with,comma\"");
    }

    @Test
    @DisplayName("updateLatestDataMessage: new message updates payload and invokes pusher")
    void testUpdateLatestDataMessage_New() throws JsonProcessingException {
        Long uid = 8L;
        String topicName = "status";
        String raw = "{\"catName\":\"Oscar\",\"temp\":22}";

        when(topicRepository.findTopicNamesByUserId(uid)).thenReturn(Optional.of(Arrays.asList(topicName)));

        when(latestDataMessageRepository.findByUserIdAndCatName(uid, "Oscar"))
                .thenReturn(Optional.empty());

        when(latestDataMessageRepository.save(any(LatestDataMessage.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(latestDataMessageRepository.findCatNamesByUserId(uid))
                .thenReturn(Collections.emptyList())
                .thenReturn(Arrays.asList("Oscar"));

        messageService.updateLatestDataMessage(uid, topicName, raw);

        ArgumentCaptor<LatestDataMessage> captor = ArgumentCaptor.forClass(LatestDataMessage.class);
        verify(latestDataMessageRepository, times(1)).save(captor.capture());
        LatestDataMessage saved = captor.getValue();
        String savedPayload = saved.getPayload();
        assertThat(savedPayload).contains("\"status\"");
        assertThat(savedPayload).contains("\"temp\":22");

        verify(pusher, times(1)).pushCatList(eq(uid), anyList());
        verify(pusher, times(1)).pushLatestMessage(eq(savedPayload), eq(uid), eq("Oscar"));
    }
}
