package CatHome.demo.service;

import CatHome.demo.exception.UserException;
import CatHome.demo.model.User;
import CatHome.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.iot.AwsIotMqttConnectionBuilder;

@Component
public class AwsConnectionFactory {
    private final UserRepository userRepository;

    @Autowired
    public AwsConnectionFactory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public MqttClientConnection createConnection(Long userId) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException("User Not Found"));

        MqttClientConnection conn = AwsIotMqttConnectionBuilder
                .newMtlsBuilder(user.getCertPem(), user.getPrivateKeyPem())
                .withEndpoint(user.getEndPoint())
                .withClientId(user.getClientId())
                .withCertificateAuthority(user.getCaPem())
                .withCleanSession(false)
                .build();

        boolean sessionPresent = conn.connect().get();
        System.out.println("MQTT connected (sessionPresent=" + sessionPresent + ")");
        return conn;
    }
}
