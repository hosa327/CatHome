package CatHome.demo.controller;
import CatHome.demo.model.User;
import CatHome.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import CatHome.demo.service.UserService;
import CatHome.demo.dto.UserInfo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @PostMapping("/users/{id}/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
            UserInfo user = userService.uploadAvatar(id, file);
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
    public ResponseEntity<UserInfo> login(@RequestBody Map<String, String> params){
        String email = params.get("email");
        String password = params.get("password");
        User user = userService.login(email, password);
        return ResponseEntity.ok(new UserInfo(user));
    }

}
