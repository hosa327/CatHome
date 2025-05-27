package CatHome.demo.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class HomeKitDataPusher {
    private final SimpMessagingTemplate template;

    @Autowired
    public HomeKitDataPusher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void push(String msg) {
        template.convertAndSend("/topic/catData", msg);
    }
}
