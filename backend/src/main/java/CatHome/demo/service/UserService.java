package CatHome.demo.service;

import CatHome.demo.dto.UserInfo;
import CatHome.demo.exception.EmailException;
import CatHome.demo.exception.UserException;
import CatHome.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import CatHome.demo.repository.UserRepository;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
//    @Autowired
//    private AvatarProperties avatarProperties;

    @Value("${BASE_URL}")
    String baseUrl;
    private BCryptPasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

    public User register(String email, String username, String password) throws IllegalAccessException {
        if (userRepository.findByEmail(email).isPresent()){
            throw new EmailException("Email already exist!");
        }
        String encodedPassword = pwdEncoder.encode(password);
        User newUser = new User(username, email, encodedPassword);
//        String defaultAvatar = avatarProperties.getPath() + "/defaultAvatar.png";
//        newUser.setAvatarRelativePath(defaultAvatar);
        User savedUser = userRepository.save(newUser);
        return savedUser;
    }


    @Value("${avatarPath}")
    private String avatarPath;

    public UserInfo uploadAvatar(Long userId, MultipartFile file)  throws IOException {
        // 1. 获取原始文件名和后缀
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null) {
            // 包含“.”才截取后缀
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);  // 包含「.」
            }
        }

        // 2. 构造新的文件名：userId + "avatar" + 后缀
        String newFileName = userId + "avatar" + extension;


        // 3. 用注入进来的 avatarPath
        File dest = new File(avatarPath, newFileName);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);

        // 4. 构造可访问的 URL：
        String relativePath = newFileName;

        // 5. 更新数据库
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setAvatarRelativePath(relativePath);
        userRepository.save(u);

        // 6. 返回给调用方
        return new UserInfo(u);
    }
    public Map<String, Boolean> checkAvailability(String username, String email) {
        boolean usernameExists = userRepository.findByUsername(username).isPresent();
        boolean emailExists    = userRepository.findByEmail(email).isPresent();

        Map<String, Boolean> result = Map.of(
                "usernameExists", usernameExists,
                "emailExists",    emailExists
        );

        return result;
    }


    public User login(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            if (pwdEncoder.matches(password, user.get().getPassword())) {
                return user.get();
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist");
        }
    }

    public UserInfo uploadMqttInfo(Long userId, String certPem, String keyPem, String caPem, String clientId, String endPoint){
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isPresent()){
            User user = optUser.get();
            user.setCertPem(certPem);
            user.setPrivateKeyPem(keyPem);
            user.setCaPem(caPem);
            user.setClientId(clientId);
            user.setEndPoint(endPoint);

            userRepository.save(user);
            return new UserInfo(user);
        }
       else{
           throw new UserException("User Not Found");
        }
    }

    public String getAvatarURL(Long userId){
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isPresent()){
            User user = optUser.get();
            String avatarRelativePath = user.getAvatarRelativePath();
            return (baseUrl + "/avatars/" + avatarRelativePath);
        } else{
            throw new UserException("User Not Found");
        }
    }

    public void changePassword(Long userId, String oldPwd, String newPwd){
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isPresent()){
            User user = optUser.get();
            if (!pwdEncoder.matches(oldPwd, user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }
            String encodedNewPwd = pwdEncoder.encode(newPwd);
            user.setPassword(encodedNewPwd);
            userRepository.save(user);
        } else{
            throw new UserException("User Not Found");
        }
    }
}
