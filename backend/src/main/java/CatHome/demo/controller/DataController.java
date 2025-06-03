package CatHome.demo.controller;


import CatHome.demo.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
public class DataController {
    private final MessageService messageService;

    @Autowired
    public DataController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/export/topic-data")
    public void exportTopicData(
            @RequestParam("userId") Long userId,
            @RequestParam("topicName") String topicName,
            @RequestParam("catName") String catName,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ConnectException("Session expired");
        }

        response.setContentType("text/csv; charset=UTF-8");

        String filename = String.format(
                "topic-%s_%s_user-%d.csv",
                URLEncoder.encode(topicName, StandardCharsets.UTF_8),
                URLEncoder.encode(catName, StandardCharsets.UTF_8),
                userId
        );

        response.setHeader(
                "Content-Disposition",
                "attachment; filename*=UTF-8''" + filename
        );

        messageService.writeFilteredTopicsAsCsv(
                response.getOutputStream(),
                userId,
                topicName,
                catName
        );
    }
}
