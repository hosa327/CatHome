package CatHome.demo.service;

import CatHome.demo.exception.ConnectionException;
import CatHome.demo.model.LatestDataMessage;
import CatHome.demo.repository.TopicMessageRepository;
import CatHome.demo.repository.TopicRepository;
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
    //    private final IoTMessageRepository messagesRepository;
    private final TopicRepository topicRepository;
    private final TopicMessageRepository topicMessageRepository;
    private final MessageService messageService;
    private MqttClientConnection connection;
    private static final Logger log = LoggerFactory.getLogger(AwsIotService.class);
    private final HomeKitDataPusher pusher;

    @Autowired
    public AwsIotService(AwsConnectionFactory connFactory,
                         MessageService messageService,
                         TopicRepository topicRepository,
                         TopicMessageRepository topicMessageRepository,
                         HomeKitDataPusher pusher) {
        this.connFactory = connFactory;
        this.messageService = messageService;
        this.topicRepository = topicRepository;
        this.topicMessageRepository = topicMessageRepository;
        this.pusher = pusher;
    }

    public void initConnection(Long userId) throws Exception {
        this.connection = connFactory.createConnection(userId);
    }

    public boolean checkStatus() {
        return connection != null;
    }




    @Transactional
    public void syncTopics(List<String> newTopics, Long userId) throws JsonProcessingException {
        if (this.connection == null){
            throw new ConnectionException("Failed to connect to AWS IoT Core");
        }

        Set<String> newSet = new HashSet<>(newTopics);
        List<String> toUnsubscribe;


        Optional<List<String>> optOldTopics =topicRepository.findTopicNamesByUserId(userId);
        if(!optOldTopics.isPresent()){
            toUnsubscribe = newSet.stream().toList();
        }else{
            List<String> oldTopics = optOldTopics.get();
            Set<String> oldSet = new HashSet<>(oldTopics);
            toUnsubscribe = oldSet.stream()
                    .filter(t -> !newSet.contains(t))
                    .toList();
        }


        //new data construction
        for (String oldTopic : toUnsubscribe) {
            messageService.deleteTopic(userId, oldTopic);
        }
        for (String newTopic : newSet) {
            messageService.addTopic(userId, newTopic);
        }

        for (String topic : toUnsubscribe) {
            try {
                connection.unsubscribe(topic).get();
                log.info("Unsubscribed {}", topic);
            } catch (Exception e) {
                log.warn("Unsubscribe {} failed", topic, e);
            }
        }

        //new data construction
        for (String newTopic : newTopics){
//            if (!oldSet.contains(newTopic)) {
            try {
                this.connection.subscribe(newTopic,
                        QualityOfService.AT_LEAST_ONCE,
                        msg -> {
                            String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
                            log.info("Received：topic={}，payload={}", msg.getTopic(), payload);
                            messageService.saveTopicMessage(userId, newTopic, payload);
                            try {
                                messageService.updateLatestDataMessage(userId, newTopic, payload);
                            } catch (Exception e) {
                                log.error("Errors when save latestData: ", e);
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





    @PreDestroy
    public void cleanup() {
        if (connection != null) {
            System.out.println("Disconnecting MQTT before shutdown...");
            connection.disconnect();
        }
    }
    // publish 方法同前面示例…
}