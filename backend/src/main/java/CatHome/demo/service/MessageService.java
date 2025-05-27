package CatHome.demo.service;

import CatHome.demo.repository.IoTMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {
    @Autowired
    private IoTMessageRepository messageRepository;

    @Transactional
    public void saveMsg(Long userId, String topic, String payload, String ts) {
        messageRepository.appendMessageWithTimestamp(userId, topic, payload, ts);

    }
}
