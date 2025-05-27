package CatHome.demo.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.crt.mqtt.MqttMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LatestDataMap {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Map<String,Object>> topics = new ConcurrentHashMap<>();
    private String catName;
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public void handleMessage(String topic, String msg) throws Exception {
        Map<String,Object> body = mapper.readValue(
                msg, new TypeReference<>() {});

        Object name = body.remove("catName");
        if (name != null) {
            this.catName = name.toString();
        }

        String now = LocalDateTime.now().format(TS_FMT);
        body.put("time", now);

        topics.put(topic, body);
    }

    public String getLatestJson() throws Exception {
        Map<String,Object> root = new LinkedHashMap<>();
        if (catName != null) {
            root.put("catName", catName);
        }
        root.putAll(topics);
        return mapper.writeValueAsString(root);
    }
}

