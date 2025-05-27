package CatHome.demo.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.crt.mqtt.MqttMessage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LatestDataMap {
    private final ObjectMapper mapper = new ObjectMapper();
    // 存 topic → payloadMap
    private final Map<String, Map<String,Object>> topics = new ConcurrentHashMap<>();
    // 全局 catName
    private String catName;

    /** 每条消息进来时调用 */
    public void handleMessage(String topic, String msg) throws Exception {
        // 扁平反序列化
        Map<String,Object> body = mapper.readValue(
                msg, new TypeReference<>() {});

        // 提取并更新 catName
        Object name = body.remove("catName");
        if (name != null) {
            this.catName = name.toString();
        }

        // 更新该 topic 的最新数据
        topics.put(topic, body);
    }

    public String getLatestJson() throws Exception {
        // 根 Map：先放 catName
        Map<String,Object> root = new LinkedHashMap<>();
        if (catName != null) {
            root.put("catName", catName);
        }
        // 再把所有 topic 数据放进去
        root.putAll(topics);
        return mapper.writeValueAsString(root);
    }
}

