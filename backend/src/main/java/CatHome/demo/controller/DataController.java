package CatHome.demo.controller;


import CatHome.demo.dto.ApiResponse;
import CatHome.demo.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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
    public ResponseEntity<ApiResponse> exportTopicData(
            @RequestParam("userId") Long userId,
            @RequestParam("topicName") String topicName,
            @RequestParam("catName") String catName,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ApiResponse apiResponse = new ApiResponse<Void>(3,"Session is invalid or has expired",null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(apiResponse);
        }

        response.setContentType("text/csv; charset=UTF-8");

        String filename = String.format(
                "topic-%s_%s_user-%d.csv",
                URLEncoder.encode(topicName, StandardCharsets.UTF_8),
                URLEncoder.encode(catName, StandardCharsets.UTF_8),
                userId
        );
        // 下面这行告诉浏览器要弹出「另存为」窗口
        response.setHeader(
                "Content-Disposition",
                "attachment; filename*=UTF-8''" + filename
        );

        // 2. 调用 Service，把筛选后的消息写到 response 的输出流
        messageService.writeFilteredTopicsAsCsv(
                response.getOutputStream(),
                userId,
                topicName,
                catName
        );
        // Servlet 容器会自动处理流的 flush/close，无需手动关闭

        ApiResponse apiResponse = new ApiResponse<Void>(0, "Export csv file successful!", null);
        return ResponseEntity.ok()
                .body(apiResponse);
    }
}
