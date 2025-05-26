package CatHome.demo.service;

import CatHome.demo.exception.ConnectionException;
import CatHome.demo.exception.UserException;
import CatHome.demo.model.User;
import CatHome.demo.model.UserMessages;
import CatHome.demo.repository.IoTMessageRepository;
import CatHome.demo.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AwsIotService {
    private final AwsConnectionFactory connFactory;
    private final IoTMessageRepository messagesRepository;
    private final MessageService messageService;
    private MqttClientConnection connection;

    @Autowired
    public AwsIotService(AwsConnectionFactory connFactory,
                         IoTMessageRepository messagesRepository,
                         MessageService messageService) {
        this.connFactory = connFactory;
        this.messagesRepository = messagesRepository;
        this.messageService = messageService;
    }



    public void initConnection(Long userId) throws Exception {
        this.connection = connFactory.createConnection(userId);
    }

    @Transactional
    public void subscribeTopic(List<String> topicList, Long userId) throws Exception{
        if (this.connection == null){
            throw new ConnectionException("Failed to connect to AWS IoT Core");
        }

        if (!messagesRepository.existsById(userId)) {
            messagesRepository.save(new UserMessages(userId));
        }

        for (String topic : topicList) {
            String subs = messagesRepository.findSubscriptions(userId);
            if (!subs.contains("\"" + topic + "\"")) {
                messagesRepository.addTopicKey(userId, topic);
            }

            this.connection.subscribe(topic,
                    QualityOfService.AT_LEAST_ONCE,
                    msg -> {
                        String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
                        String receivedAt =
                                java.time.LocalDateTime
                                        .now()
                                        .format(java.time.format.DateTimeFormatter.ISO_DATE_TIME);
                        System.out.printf("Received：topic=%s，payload=%s%n", msg.getTopic(), payload);

                        try {
                            messageService.saveMsg(userId, topic, payload, receivedAt);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

            ).get();
        }

        System.out.println("Subscribed：" + topicList);
    }

    @Transactional
    public void syncTopics(List<String> newTopics, Long userId) throws JsonProcessingException {
        String oldTopics = messagesRepository.findSubscriptions(userId);
        Map<String, ?> oldMap = new ObjectMapper().readValue(oldTopics, new TypeReference<Map<String, ?>>(){});
        Set<String> oldSet = oldMap.keySet();

        Set<String> newSet = new HashSet<>(newTopics);

        for (String oldTopic : oldSet) {
            if (!newSet.contains(oldTopic)) {
                messagesRepository.removeTopicKey(userId, oldTopic);
                connection.unsubscribe(oldTopic);
            }
        }

        for (String newTopic : newSet) {
            if (!oldSet.contains(newTopic)) {
                messagesRepository.addTopicKey(userId, newTopic);
            }
        }
    }

    public Map<String, Object> getSubscribedTopics(Long userId) throws JsonProcessingException {
        String subscription = messagesRepository.findSubscriptions(userId);
        Map<String, Object> map = new ObjectMapper().readValue(subscription, new TypeReference<>(){});
        return map;
    }

    @PreDestroy
    public void cleanup() {
        if (connection != null) {
            System.out.println("Disconnecting MQTT before shutdown...");
            connection.disconnect();
        }
    }
    // publish 方法同前面示例…
}
