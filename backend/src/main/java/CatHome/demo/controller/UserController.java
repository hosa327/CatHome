package CatHome.demo.controller;
import CatHome.demo.dto.ApiResponse;
import CatHome.demo.model.User;
import CatHome.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import CatHome.demo.service.UserService;
import CatHome.demo.dto.UserInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserInfo> register(@RequestBody Map<String, String> params) throws IllegalAccessException {
        String username = params.get("username");
        String email = params.get("email");
        String password = params.get("password");
        User user = userService.register(email, username, password);
        System.out.println("user saved");
        return  ResponseEntity.ok(new UserInfo(user));
    }

    @PostMapping("/users/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            HttpSession session,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        Long userId = (Long) session.getAttribute("userId");

        UserInfo user = userService.uploadAvatar(userId, file);
        return ResponseEntity.ok(Map.of("avatarRelativePath", user.getAvatarRelativePath()));
    }

    @PostMapping("/check-availability")
    public ResponseEntity<Map<String, Boolean>> checkAvailability(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String email    = payload.get("email");

        Map<String, Boolean> result = userService.checkAvailability(username, email);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<UserInfo> login(@RequestBody Map<String, String> params, HttpServletRequest req){
        String email = params.get("email");
        String password = params.get("password");

        User user = userService.login(email, password);

        HttpSession session = req.getSession(true);
        session.setAttribute("userId", user.getId());

        return ResponseEntity.ok(new UserInfo(user));
    }

    @GetMapping("/userInfo")
    public ResponseEntity<?> getAvatar(HttpServletRequest request, @RequestParam List<String> info) throws IOException {;
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            ApiResponse response = new ApiResponse<Void>(0, "Session is invalid or has expired", null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).
                    body(response);
        }
        Long userId = (Long) session.getAttribute("userId");

        Map<String,Object> result = new HashMap<>();
        for(String type: info){
            switch (type){
                case "avatar":
                    String avatarUrl = userService.getAvatarURL(userId);
                    result.put("avatarURL", avatarUrl);
                    break;
                case "userId":
                    result.put("userId", userId);
                    break;
            }
        }
        return ResponseEntity.ok(result);
    }
}
