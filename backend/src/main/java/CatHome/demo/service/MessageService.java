package CatHome.demo.service;

import CatHome.demo.exception.TopicException;
import CatHome.demo.model.*;
import CatHome.demo.repository.LatestDataMessageRepository;
import CatHome.demo.repository.TopicRepository;
import CatHome.demo.repository.UserMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {
    private final UserMessageRepository userMessageRepository;
    private final TopicRepository topicRepository;
    private final LatestDataMessageRepository latestDataMessageRepository;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    private final HomeKitDataPusher pusher;
    public MessageService(UserMessageRepository userMessageRepository,
                          TopicRepository topicRepository,
                          LatestDataMessageRepository latestDataMessageRepository,
                          ObjectMapper objectMapper, HomeKitDataPusher pusher) {
        this.userMessageRepository = userMessageRepository;
        this.topicRepository = topicRepository;
        this.latestDataMessageRepository = latestDataMessageRepository;
        this.objectMapper = objectMapper;
        this.pusher = pusher;
    }

    public Map<String, String> separateMessage(String msg) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(msg);
            String catName = root.get("catName").asText();
            ((ObjectNode) root).remove("catName");
            String remainingJson = mapper.writeValueAsString(root);

            Map<String, String> result = new HashMap<>();
            result.put("catName", catName);
            result.put("payload", remainingJson);
            return result;
        } catch (JsonProcessingException e) {
            throw new TopicException("Parsing message failed：" + e.getOriginalMessage());
        }
    }

    @Transactional
    public UserMessages addTopic(Long userId, String topicName) {
        Optional<UserMessages> optUser = userMessageRepository.findById(userId);
        UserMessages user;
        if(!optUser.isPresent()) {
            user = new UserMessages(userId);
            userMessageRepository.save(user);
            Topic topic = new Topic(topicName);
            user.addTopic(topic);

        }else{
            user = optUser.get();
            if(!topicRepository.findTopicByUserIdAndTopicName(userId, topicName).isPresent()){
                Topic topic = new Topic(topicName);
                user.addTopic(topic);
            }
        }
        return userMessageRepository.save(user);
    }

    @Transactional
    public UserMessages deleteTopic(Long userId, String topicName) {
        Optional<UserMessages> optUser = userMessageRepository.findById(userId);
        UserMessages user;
        if(!optUser.isPresent()) {
            user = new UserMessages(userId);
        }else{
            user = optUser.get();
            Optional<Topic> optTopic = topicRepository.findTopicByUserIdAndTopicName(userId, topicName);
            if (optTopic.isPresent()) {
                Topic topic = optTopic.get();
                user.removeTopic(topic);
            }
        }
        return userMessageRepository.save(user);
    }

    public List<String> getTopics(Long userId){
        Optional<List<String>> optTopicList = topicRepository.findTopicNamesByUserId(userId);
        List<String> topicList;
        if(!optTopicList.isPresent()){
            topicList = new ArrayList<>();;
        }else{
            topicList = optTopicList.get();
        } return topicList;
    }


    private Topic addMessageToTopic(String topicName, Long userId, String message) {
        Map<String, String> messageMap = separateMessage(message);
        String catName = messageMap.get("catName");
        String payload = messageMap.get("payload");

        Optional<Topic> optTopic = topicRepository.findTopicByUserIdAndTopicName(userId, topicName);
        if(!optTopic.isPresent()){
            throw new TopicException("Topic Not Found");
        }else{
            Topic topic = optTopic.get();
            LocalDateTime localDateTime = LocalDateTime.now();
            TopicMessage topicMessage = new TopicMessage(catName, payload, localDateTime);
            topic.addMessage(topicMessage);
            return topic;
        }
    }

    @Transactional
    public Topic saveTopicMessage(
            Long userId,
            String topicName,
            String message
    ) {
        Optional<UserMessages> optUser = userMessageRepository.findById(userId);
        UserMessages user;
        if (!optUser.isPresent()) {
            user = new UserMessages(userId);
            userMessageRepository.save(user);
        } else {
            user = optUser.get();
        }
        Topic topic = addMessageToTopic(topicName, userId, message);
        return topicRepository.save(topic);
    }

    @Transactional
    public void updateLatestDataMessage(Long userId, String topicName, String newMessage){
        Map<String, String> messageMap = separateMessage(newMessage);
        Optional<List<String>> optTopicList = topicRepository.findTopicNamesByUserId(userId);
        List<String> topicList = optTopicList.get();

        String catName = messageMap.get("catName");
        String newPayload = messageMap.get("payload");

        Optional<LatestDataMessage> optMsg = latestDataMessageRepository.findByUserIdAndCatName(userId,catName);
        LatestDataMessage msg;
        if(!optMsg.isPresent()){
            msg = new LatestDataMessage(userId, catName);
        }else{
            msg = optMsg.get();
        }
        Map<String, Object> payloadMap = new HashMap<>();
        String oldPayload = msg.getPayload();
        try {
            payloadMap = objectMapper.readValue(
                    oldPayload,
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (JsonProcessingException e) {
            payloadMap = new HashMap<>();
        }
        try {
            Map<String, Object> newPayloadMap = objectMapper.readValue(
                    newPayload,
                    new TypeReference<Map<String, Object>>() {}
            );
            if(topicList.contains(topicName)){
                if (payloadMap.containsKey(topicName)) {
                    payloadMap.put(topicName, newPayloadMap);
                } else {
                    payloadMap.put(topicName, newPayloadMap);
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Parsing payload failed: " + e.getOriginalMessage(), e);
        }
        try {
            msg.setPayload(objectMapper.writeValueAsString(payloadMap));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Parsing payload failed: " + e.getOriginalMessage(), e);
        }

        String latestMessage = msg.getPayload();
        log.info("Latest Message: latestMessage={}", latestMessage);
        pusher.pushLatestMessage(latestMessage, userId);
    }
}