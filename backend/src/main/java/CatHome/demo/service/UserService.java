package CatHome.demo.service;

import CatHome.demo.dto.UserInfo;
import CatHome.demo.exception.EmailException;
import CatHome.demo.exception.UserException;
import CatHome.demo.model.LatestDataMessage;
import CatHome.demo.model.User;
import CatHome.demo.repository.LatestDataMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
    private final UserRepository userRepository;
    private final HomeKitDataPusher pusher;
    private final LatestDataMessageRepository latestDataMessageRepository;

    @Autowired
    public UserService(UserRepository userRepository,
                       HomeKitDataPusher pusher,
                       LatestDataMessageRepository latestDataMessageRepository) {
        this.userRepository = userRepository;
        this.pusher = pusher;
        this.latestDataMessageRepository = latestDataMessageRepository;
    }


    @Value("${BASE_URL}")
    String baseUrl;
    private BCryptPasswordEncoder pwdEncoder = new BCryptPasswordEncoder();

    public User register(String email, String username, String password) throws IllegalAccessException {
        if (userRepository.findByEmail(email).isPresent()){
            throw new EmailException("Email already exist!");
        }
        String encodedPassword = pwdEncoder.encode(password);
        User newUser = new User(username, email, encodedPassword);
        User savedUser = userRepository.save(newUser);
        return savedUser;
    }


    @Value("${avatarPath}")
    private String avatarPath;

    public UserInfo uploadAvatar(Long userId, MultipartFile file)  throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }
        }

        String newFileName = userId + "avatar" + extension;

        File dest = new File(avatarPath, newFileName);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);

        String relativePath = newFileName;

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setAvatarRelativePath(relativePath);
        userRepository.save(u);

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

    public String getUserName(Long userId){
        Optional<User> optUser = userRepository.findById(userId);
        if (optUser.isPresent()){
            User user = optUser.get();
            String userName = user.getUserName();
            return (userName);
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
