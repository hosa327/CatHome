package CatHome.demo.service;

import CatHome.demo.exception.ConnectionException;
import CatHome.demo.model.HomeKitData;
import CatHome.demo.model.UserMessages;
import CatHome.demo.repository.IoTMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.QualityOfService;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AwsIotService {
    private final AwsConnectionFactory connFactory;
    private final IoTMessageRepository messagesRepository;
    private final MessageService messageService;
    private MqttClientConnection connection;
    private static final Logger log = LoggerFactory.getLogger(AwsIotService.class);

    @Autowired
    public AwsIotService(AwsConnectionFactory connFactory,
                         IoTMessageRepository messagesRepository,
                         MessageService messageService) {
        this.connFactory = connFactory;
        this.messagesRepository = messagesRepository;
        this.messageService = messageService;
    }
    @Autowired
    private HomeKitDataPusher pusher;



    public void initConnection(Long userId) throws Exception {
        this.connection = connFactory.createConnection(userId);
    }


    @Transactional
    public void syncTopics(List<String> newTopics, Long userId) throws JsonProcessingException {
        String oldTopics = messagesRepository.findSubscriptions(userId);
        Map<String, ?> oldMap = new ObjectMapper().readValue(oldTopics, new TypeReference<Map<String, ?>>(){});
        Set<String> oldSet = oldMap.keySet();
        Set<String> newSet = new HashSet<>(newTopics);

        if (this.connection == null){
            throw new ConnectionException("Failed to connect to AWS IoT Core");
        }

        if (!messagesRepository.existsById(userId)) {
            messagesRepository.save(new UserMessages(userId));
        }

        List<String> toUnsubscribe = oldSet.stream()
                .filter(t -> !newSet.contains(t))
                .toList();
//        List<String> toSubscribe = newSet.stream()
//                .filter(t -> !oldSet.contains(t))
//                .toList();


        for (String oldTopic : toUnsubscribe) {
            messagesRepository.removeTopicKey(userId, oldTopic);
        }
        for (String newTopic : newSet) {
            messagesRepository.addTopicKey(userId, newTopic);
        }

        for (String topic : toUnsubscribe) {
            try {
                connection.unsubscribe(topic).get();
                log.info("Unsubscribed {}", topic);
            } catch (Exception e) {
                log.warn("Unsubscribe {} failed", topic, e);
            }
        }
        for (String newTopic : newTopics){
//            if (!oldSet.contains(newTopic)) {
                try {
                    this.connection.subscribe(newTopic,
                            QualityOfService.AT_LEAST_ONCE,
                            msg -> {
                                String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
                                String receivedAt =
                                        java.time.LocalDateTime
                                                .now()
                                                .format(java.time.format.DateTimeFormatter.ISO_DATE_TIME);
                                log.info("Received：topic={}，payload={}", msg.getTopic(), payload);
                                messageService.saveMsg(userId, newTopic, payload, receivedAt);
                                try {
                                    HomeKitData pushData = parsePayload(payload);
                                    pusher.push(pushData);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                    ).get();
                    log.info("subscribe {}", newTopic);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
//    }

    private HomeKitData parsePayload(String json) throws JsonProcessingException {
        return new ObjectMapper().readValue(json, HomeKitData.class);
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
