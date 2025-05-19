package CatHome.demo.service;

import CatHome.demo.exception.ConnectionException;
import CatHome.demo.exception.UserException;
import CatHome.demo.model.User;
import CatHome.demo.model.UserMessages;
import CatHome.demo.repository.IoTMessageRepository;
import CatHome.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class AwsIotService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private IoTMessageRepository messagesRepository;
    @Autowired
    private MessageService messageService;


    private MqttClientConnection connection;
    private String subscribedTopic;

    public AwsIotService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public void initConnection(
            Long userId
    ) throws Exception {
        String topic = "arduino_outgoing";

        Optional<User> optUser = userRepository.findById(userId);
        if(optUser.isPresent()){
            User user = optUser.get();
            String endpoint = user.getEndPoint();
            String clientId = user.getClientId();
//            String topic = user.getTopic();
            String certPem = user.getCertPem();
            String keyPem = user.getPrivateKeyPem();
            String caPem = user.getCaPem();

            // connect to aws
            this.connection = AwsIotMqttConnectionBuilder
                    .newMtlsBuilder(certPem, keyPem)
                    .withEndpoint(endpoint)
                    .withClientId(clientId)
                    .withCertificateAuthority(caPem)
//                .withCleanSession(true)
//                .withConnectionEventCallbacks(callbacks)
                    .build();

            boolean sessionPresent = this.connection.connect().get();
            System.out.println("MQTT connected，sessionPresent=" + sessionPresent);

        }
        else{
            throw new UserException("User Not Found");
        }
    }

    public void subscribeTopic(String topic, Long userId) throws Exception{
        if (this.connection == null){
            throw new ConnectionException("Failed to connect to AWS IoT Core");
        }

        if (!messagesRepository.existsById(userId)) {
            messagesRepository.save(new UserMessages(userId));
        }

        messagesRepository.addTopicKey(userId, topic);

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

        this.subscribedTopic = topic;
        System.out.println("Subscribed：" + topic);


    }


    // publish 方法同前面示例…
}
