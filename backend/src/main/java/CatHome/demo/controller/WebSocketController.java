package CatHome.demo.controller;

import CatHome.demo.model.LatestDataMessage;
import CatHome.demo.repository.LatestDataMessageRepository;
import CatHome.demo.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class WebSocketController {
    private final SimpMessagingTemplate template;
    private final LatestDataMessageRepository latestDataMessageRepository;
    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    public WebSocketController(SimpMessagingTemplate template,
                               LatestDataMessageRepository latestDataMessageRepository) {
        this.template = template;
        this.latestDataMessageRepository = latestDataMessageRepository;
    }

    @MessageMapping("/requestCatList")
    public void onRequestCatList(@Payload Map<String, String> payload){
        String userIdStr  = payload.get("userId");
        Long userId = Long.valueOf(userIdStr);

        List<String> catList = latestDataMessageRepository.findCatNamesByUserId(userId);
        template.convertAndSend("/topic/" + userId +"/catList", catList);
    }

    @MessageMapping("/requestLatest")
    public void onRequestLatest(@Payload Map<String,String> payload) {
        String userIdStr  = payload.get("userId");
        String catName = payload.get("catName");

        Long userId = Long.valueOf(userIdStr);

        Optional<LatestDataMessage> msg = latestDataMessageRepository.findByUserIdAndCatName(userId, catName);
        if (msg.isPresent()) {
            template.convertAndSend("/topic/" + userId +"/"+ catName, msg.get().getPayload());
        }
    }
}