package CatHome.demo.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeKitDataPusher {
    private final SimpMessagingTemplate template;

    @Autowired
    public HomeKitDataPusher(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void pushLatestMessage(String msg, Long userId, String catName) {
        template.convertAndSend("/topic/"+ userId +"/" + catName, msg);
    }

    public void pushCatList(Long userId, List<String> catList){
        template.convertAndSend("/topic/"+ userId + "/catList", catList);
    }
}