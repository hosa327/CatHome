//package CatHome.demo.controller;
//
//
//import CatHome.demo.dto.UserInfo;
//import CatHome.demo.exception.UserException;
//import CatHome.demo.model.User;
//import CatHome.demo.repository.UserRepository;
//import CatHome.demo.service.AwsIotService;
//import CatHome.demo.service.UserService;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpSession;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.*;
//
//@RestController
//@RequestMapping("/home")
//public class HomeController {
//    @Autowired
//    private UserService userService;
//
//    @GetMapping("/userInfo")
//    public ResponseEntity<Map<String, Object>> getAvatar(HttpServletRequest request, @RequestParam List<String> info) throws IOException {;
//        HttpSession session = request.getSession(false);
//        if (session == null || session.getAttribute("userId") == null) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//        Long userId = (Long) session.getAttribute("userId");
//
//        Map<String,Object> result = new HashMap<>();
//        for(String type: info){
//            switch (type){
//                case "avatar":
//                    String avatarUrl = userService.getAvatarURL(userId);
//                    result.put("avatarURL", avatarUrl);
//                    break;
//                case "userId":
//                    result.put("userId", userId);
//                    break;
//            }
//        }
//
//        return ResponseEntity.ok(result);
//    }
//}
