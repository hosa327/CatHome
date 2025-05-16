package CatHome.demo.service;

import CatHome.demo.model.User;
import CatHome.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

import java.nio.charset.StandardCharsets;

@Service
public class AwsIotService {
    @Autowired
    private UserRepository userRepository;
    private MqttClientConnection connection;
    private String subscribedTopic;

    public AwsIotService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public synchronized void initConnectionForUser(
            Long userId, String endpoint, String clientId, String topic
    ) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User Not Found"));

        String certPem    = user.getCertPem();
        String keyPem  = user.getPrivateKeyPem();
        String caPem  = user.getCaPem();

        // 重用前面演示的 initConnection 逻辑
        initConnection(endpoint, clientId, topic, certPem, keyPem, caPem);
    }

    public void initConnection(
            String endpoint, String clientId, String topic,
            String certPem, String keyPem, String caPem
    ) throws Exception {
        topic = "GPS_Location";

        // 构建并连接
        connection = AwsIotMqttConnectionBuilder
                .newMtlsBuilder(certPem, keyPem)
                .withEndpoint(endpoint)
                .withClientId(clientId)
                .withCertificateAuthority(caPem)
//                .withCleanSession(true)
//                .withConnectionEventCallbacks(callbacks)
                .build();

        boolean sessionPresent = connection.connect().get();
        System.out.println("MQTT connected，sessionPresent=" + sessionPresent);

        connection.subscribe(
                topic,
                QualityOfService.AT_LEAST_ONCE,
                msg -> {
                    // 当有消息到达时，这段回调会被调用
                    String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
                    System.out.printf("Received：topic=%s，payload=%s%n", msg.getTopic(), payload);
                }
        ).get();
        subscribedTopic = topic;  // 记录已订阅的主题
        System.out.println("Subscribed：" + topic);

    }

    // publish 方法同前面示例…
}
