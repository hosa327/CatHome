package CatHome.demo.controller;

import CatHome.demo.model.LatestDataMessage;
import CatHome.demo.repository.LatestDataMessageRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@Controller
public class WebSocketController {
    private final SimpMessagingTemplate template;
    private final LatestDataMessageRepository latestDataMessageRepository;

    public WebSocketController(SimpMessagingTemplate template,
                               LatestDataMessageRepository latestDataMessageRepository) {
        this.template = template;
        this.latestDataMessageRepository = latestDataMessageRepository;
    }

    @MessageMapping("/requestLatest")
    public void onRequestLatest(@Payload Map<String,String> payload, Principal principal) {
        Long userId  = Long.valueOf(payload.get("userId"));
        String catName = payload.get("catName");

        Optional<LatestDataMessage> msg = latestDataMessageRepository.findByUserIdAndCatName(userId, catName);
        if (msg.isPresent()) {
            template.convertAndSend("/topic/catData", msg.get().getPayload());
        }
    }
}
