package CatHome.demo.controller;

import CatHome.demo.dto.ApiResponse;
import CatHome.demo.service.AwsIotService;
import CatHome.demo.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/mqtt")
public class MqttController {
    @Autowired
    private UserService userService;

    @Autowired
    private AwsIotService IotService;

    @PostMapping(
            path = "/config/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadAWSinfo(@RequestParam String clientId,
                                              @RequestParam String endPoint,
                                              @RequestPart MultipartFile clientCert,
                                              @RequestPart MultipartFile clientKey,
                                              @RequestPart MultipartFile caCert,
                                              HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            ApiResponse response = new ApiResponse<Void>(3,"Session is invalid or has expired",null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }
        Long userId = (Long) session.getAttribute("userId");

        String certPem       = new String(clientCert.getBytes(), StandardCharsets.UTF_8);
        String privateKeyPem = new String(clientKey.getBytes(),  StandardCharsets.UTF_8);
        String caPem         = new String(caCert.getBytes(),  StandardCharsets.UTF_8);

        userService.uploadMqttInfo(userId, certPem, privateKeyPem, caPem, clientId, endPoint);


        try {
            IotService.initConnection(userId);
            System.out.print("Connection Successful");
            ApiResponse response = new ApiResponse<Void>(1, "Connection Successful", null);
            return ResponseEntity.ok()
                    .body(response);
        } catch (Exception e) {
            System.err.println("Initial Failed：" + e.getMessage());
            ApiResponse response = new ApiResponse<Void> (0, "Initial Failed：" + e.getMessage(), null);
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PostMapping("/awsConnect")
    public ResponseEntity<ApiResponse> awsConnect(HttpServletRequest request){
        HttpSession session = request.getSession(false);
        if (session == null) {
            ApiResponse response = new ApiResponse<Void>(3,"Session is invalid or has expired",null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        Long userId = (Long) session.getAttribute("userId");
        System.out.println(session);

        try {
            IotService.initConnection(userId);
            System.out.print("Connection Successful");
            ApiResponse response = new ApiResponse<Void>(1, "Connection Successful", null);
            return ResponseEntity.ok()
                    .body(response);
        } catch (Exception e) {
            System.err.println("Initial Failed：" + e.getMessage());
            e.printStackTrace();

            ApiResponse response = new ApiResponse<Void>(0, "error:"+ e.getMessage(), null);
            return ResponseEntity.
                    status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PostMapping("/subscribeTopic")
    public ResponseEntity<?> setTopic(HttpServletRequest request,
                                      @RequestBody List<String> topicList){
        HttpSession session = request.getSession(false);
        System.out.print(topicList);
        if (session == null) {
            ApiResponse response = new ApiResponse<Void>(3,"Session is invalid or has expired",null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }
        Long userId = (Long) session.getAttribute("userId");
        try {
            IotService.syncTopics(topicList, userId);
            ApiResponse response = new ApiResponse<Void>(1, "Subscribed to topics: " + topicList, null);
            return ResponseEntity.ok()
                    .body(response);
        } catch (Exception e) {
            ApiResponse response = new ApiResponse<Void>(0, "Failed to subscribe: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<ApiResponse<?>> getTopics(HttpServletRequest request){
        HttpSession session = request.getSession(false);
        if (session == null) {
            ApiResponse response = new ApiResponse<Void>(3,"Session is invalid or has expired",null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }

        Long userId = (Long) session.getAttribute("userId");
        try {
            Map<String, Object> subscriptions = IotService.getSubscribedTopics(userId);
            ApiResponse<ArrayList> response = new ApiResponse<ArrayList>(1, "Get topics successful.", new ArrayList<>(subscriptions.keySet()));
            return ResponseEntity.ok()
                    .body(response);

        }catch (JsonProcessingException e){
            ApiResponse<Void> response = new ApiResponse<>(5, "Failed to parse subscription data", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

//    @MessageMapping("/requestLatest")
//    @SendTo("/topic/catData")
//    public HomeKitData handleRequestLatest(){
//
//        return
//    }
}
