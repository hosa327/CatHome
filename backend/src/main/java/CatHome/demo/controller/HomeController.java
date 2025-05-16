package CatHome.demo.controller;


import CatHome.demo.dto.UserInfo;
import CatHome.demo.exception.UserException;
import CatHome.demo.model.User;
import CatHome.demo.repository.UserRepository;
import CatHome.demo.service.AwsIotService;
import CatHome.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

@RestController
@RequestMapping("/home")
public class HomeController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AwsIotService IotService;

    @Value("${avatarPath}")
    private String avatarPath;

//    @GetMapping("/home")
//    public
//    ResponseEntity<Long> home(@RequestBody Map<StringJoiner, long> params){
//        Long userId = params.get("userId");
//
//        return  ResponseEntity.ok(userId);
//    }

    @GetMapping("api/{id}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long id) throws IOException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String relativePath = user.getAvatarRelativePath();

        if (relativePath == null || relativePath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No avatar set");
        }

        // 2. 构造绝对路径并包装成 Resource
        Path file = Paths.get(avatarPath).resolve(relativePath).normalize();
        if (!Files.exists(file) || !Files.isReadable(file)) {
            relativePath = "defaultAvatar.png";
            file = Paths.get(avatarPath).resolve(relativePath).normalize();

            if (!Files.exists(file) || !Files.isReadable(file)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Avatar not found and default avatar missing");
            }
        }
        Resource resource = new UrlResource(file.toUri());

        // 3. 自动探测文件类型（也可以根据后缀硬编码为 image/png、image/jpeg 等）
        String contentType = Files.probeContentType(file);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // 4. 返回带 Content-Type 的文件流
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping("mqtt/config/upload")
    public ResponseEntity<UserInfo> uploadAWSinfo(@RequestPart("clientCert") MultipartFile clientCert,
                                                  @RequestPart("clientKey")  MultipartFile clientKey,
                                                  @RequestPart(value = "caCert", required = false) MultipartFile caCert,
                                                  @RequestPart("clientId") String clientId,
                                                  @RequestPart("endPoint") String endPoint) throws IOException {

        String certPem       = new String(clientCert.getBytes(), StandardCharsets.UTF_8);
        String privateKeyPem = new String(clientKey.getBytes(),  StandardCharsets.UTF_8);
        String caPem         = (caCert != null)
                ? new String(caCert.getBytes(),  StandardCharsets.UTF_8)
                : null;

        User user = userRepository.findByUsername("1")
                .orElseThrow(() -> new UserException("User Not Found"));

        userService.uploadMqttInfo(user.getId(), certPem, privateKeyPem, caPem);


        try {
            IotService.initConnection(
                    endPoint,
                    clientId,
                    "",
                    certPem,
                    privateKeyPem,
                    caPem
            );
        } catch (Exception e) {
            System.err.println("初始化 AWS IoT 连接失败：" + e.getMessage());
            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserInfo(user));
        }

        // 5. 返回
        return ResponseEntity.ok(new UserInfo(user));
    }

//
//        Optional<User> user = userRepository.findByUsername("1");
//        if(user.isPresent()) {
//            Long userId = user.get().getId();
//            userService.uploadMqttInfo(userId, certPem, privateKeyPem, caPem);
//            IotService.initConnectionForUser(userId, endPoint, clientId, "") ;
//            return ResponseEntity.ok(new UserInfo(user.get()));
//            }
//        else{
//            System.out.print("User not found");
//            throw new UserException("User Not Found");
//        }
//    }
}
