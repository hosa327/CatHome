// src/test/java/CatHome/demo/service/UserServiceTest.java
package CatHome.demo.service;

import CatHome.demo.dto.UserInfo;
import CatHome.demo.exception.EmailException;
import CatHome.demo.exception.UserException;
import CatHome.demo.model.User;
import CatHome.demo.repository.LatestDataMessageRepository;
import CatHome.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HomeKitDataPusher pusher;

    @Mock
    private LatestDataMessageRepository latestDataMessageRepository;

    @InjectMocks
    private UserService userService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        // Inject private @Value fields via ReflectionTestUtils
        ReflectionTestUtils.setField(userService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(userService, "avatarPath", "target/test-avatars");
    }

    @Test
    @DisplayName("register(): when email already exists, throw EmailException")
    void testRegister_EmailExists_ThrowsEmailException() {
        String email = "a@example.com";
        String username = "alice";
        String rawPassword = "password123";

        // Simulate that findByEmail(email) returns a User (email already exists)
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(new User(username, email, encoder.encode(rawPassword))));

        // Expect EmailException with message "Email already exist!"
        assertThatThrownBy(() -> userService.register(email, username, rawPassword))
                .isInstanceOf(EmailException.class)
                .hasMessage("Email already exist!");

        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register(): successful registration returns saved User")
    void testRegister_Success() throws NoSuchFieldException, IllegalAccessException {
        String email = "b@example.com";
        String username = "bob";
        String rawPassword = "mysecret";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When saving, assign id = 100L to the new User instance
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            Field idField = toSave.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(toSave, 100L);
            return toSave;
        });

        User result = userService.register(email, username, rawPassword);

        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, times(1)).save(any(User.class));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getUserName()).isEqualTo(username);
        // Password should be encrypted, not equal to rawPassword
        assertThat(result.getPassword()).isNotEqualTo(rawPassword);
    }

    @Test
    @DisplayName("uploadAvatar(): after uploading avatar, returns UserInfo and sets avatarRelativePath")
    void testUploadAvatar_Success() throws IOException, NoSuchFieldException, IllegalAccessException {
        Long userId = 1L;
        User existingUser = new User();
        // Use reflection to set the private id field to 1L
        Field idField = existingUser.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(existingUser, userId);
        existingUser.setAvatarRelativePath(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        byte[] fileContent = "dummy".getBytes();
        MockMultipartFile mockFile = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                fileContent
        );

        UserInfo info = userService.uploadAvatar(userId, mockFile);

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(any(User.class));

        // After upload, avatarRelativePath should be "1avatar.png"
        assertThat(existingUser.getAvatarRelativePath()).isEqualTo("1avatar.png");
        assertThat(info).isNotNull();
        assertThat(info.getId()).isEqualTo(userId);
        assertThat(info.getAvatarRelativePath()).isEqualTo("1avatar.png");
    }

    @Test
    @DisplayName("checkAvailability(): returns map indicating whether username/email exist")
    void testCheckAvailability() {
        String username = "charlie";
        String email = "c@example.com";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User()));

        Map<String, Boolean> result = userService.checkAvailability(username, email);

        assertThat(result).containsEntry("usernameExists", false);
        assertThat(result).containsEntry("emailExists", true);

        verify(userRepository, times(1)).findByUsername(username);
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("login(): when user does not exist, throw ResponseStatusException(404)")
    void testLogin_UserNotFound() {
        String email = "d@example.com";
        String password = "whatever";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(email, password))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).isEqualTo("User does not exist");
                });

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("login(): when password is incorrect, throw ResponseStatusException(401)")
    void testLogin_WrongPassword() {
        String email = "e@example.com";
        String rawPassword = "correctpwd";
        String wrongPassword = "wrongpwd";

        String encoded = encoder.encode(rawPassword);
        User dbUser = new User("frank", email, encoded);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dbUser));

        assertThatThrownBy(() -> userService.login(email, wrongPassword))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).isEqualTo("Incorrect password");
                });

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("login(): returns User when email and password are correct")
    void testLogin_Success() {
        String email = "g@example.com";
        String rawPassword = "mypassword";
        String encoded = encoder.encode(rawPassword);
        User dbUser = new User("george", email, encoded);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dbUser));

        User result = userService.login(email, rawPassword);

        verify(userRepository, times(1)).findByEmail(email);
        assertThat(result).isEqualTo(dbUser);
    }

    @Test
    @DisplayName("uploadMqttInfo(): when user ID not found, throw UserException")
    void testUploadMqttInfo_UserNotFound() {
        Long uid = 10L;
        when(userRepository.findById(uid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.uploadMqttInfo(uid, "cert", "key", "ca", "cid", "ep"))
                .isInstanceOf(UserException.class)
                .hasMessage("User Not Found");

        verify(userRepository, times(1)).findById(uid);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadMqttInfo(): when user exists, returns UserInfo with id, email, and name")
    void testUploadMqttInfo_Success() throws NoSuchFieldException, IllegalAccessException {
        Long uid = 2L;
        User dbUser = new User("helen", "h@example.com", encoder.encode("pwd"));
        Field idField = dbUser.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(dbUser, uid);

        when(userRepository.findById(uid)).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserInfo info = userService.uploadMqttInfo(
                uid,
                "certPem_dummy",
                "keyPem_dummy",
                "caPem_dummy",
                "clientidX",
                "endpointY"
        );

        verify(userRepository, times(1)).findById(uid);
        verify(userRepository, times(1)).save(any(User.class));

        assertThat(info).isNotNull();
        assertThat(info.getId()).isEqualTo(uid);
        assertThat(info.getEmail()).isEqualTo("h@example.com");
        assertThat(info.getName()).isEqualTo("helen");
        // avatarRelativePath should remain null because uploadMqttInfo does not modify it
        assertThat(info.getAvatarRelativePath()).isNull();
    }

    @Test
    @DisplayName("getAvatarURL(): when user not found, throw UserException")
    void testGetAvatarURL_UserNotFound() {
        Long uid = 5L;
        when(userRepository.findById(uid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getAvatarURL(uid))
                .isInstanceOf(UserException.class)
                .hasMessage("User Not Found");

        verify(userRepository, times(1)).findById(uid);
    }

    @Test
    @DisplayName("getAvatarURL(): when user exists, return full URL")
    void testGetAvatarURL_Success() throws NoSuchFieldException, IllegalAccessException {
        Long uid = 6L;
        User dbUser = new User("ivy", "i@example.com", encoder.encode("pwd"));
        Field idField = dbUser.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(dbUser, uid);
        dbUser.setAvatarRelativePath("6avatar.jpg");

        when(userRepository.findById(uid)).thenReturn(Optional.of(dbUser));

        String url = userService.getAvatarURL(uid);
        verify(userRepository, times(1)).findById(uid);

        assertThat(url).isEqualTo("http://localhost:8080/avatars/6avatar.jpg");
    }

    @Test
    @DisplayName("getUserName(): when user not found, throw UserException")
    void testGetUserName_UserNotFound() {
        Long uid = 7L;
        when(userRepository.findById(uid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserName(uid))
                .isInstanceOf(UserException.class)
                .hasMessage("User Not Found");

        verify(userRepository, times(1)).findById(uid);
    }

    @Test
    @DisplayName("getUserName(): when user exists, return username")
    void testGetUserName_Success() throws NoSuchFieldException, IllegalAccessException {
        Long uid = 8L;
        User dbUser = new User("jack", "j@example.com", encoder.encode("pwd"));
        Field idField = dbUser.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(dbUser, uid);

        when(userRepository.findById(uid)).thenReturn(Optional.of(dbUser));

        String name = userService.getUserName(uid);
        verify(userRepository, times(1)).findById(uid);

        assertThat(name).isEqualTo("jack");
    }

    @Test
    @DisplayName("changePassword(): when user not found, throw UserException")
    void testChangePassword_UserNotFound() {
        Long uid = 9L;
        when(userRepository.findById(uid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(uid, "oldPwd", "newPwd"))
                .isInstanceOf(UserException.class)
                .hasMessage("User Not Found");

        verify(userRepository, times(1)).findById(uid);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword(): when old password is incorrect, throw RuntimeException")
    void testChangePassword_WrongOldPassword() throws NoSuchFieldException, IllegalAccessException {
        Long uid = 11L;
        String correctOld = "correctOld";
        String encodedOld = encoder.encode(correctOld);
        User dbUser = new User("kate", "k@example.com", encodedOld);
        Field idField = dbUser.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(dbUser, uid);

        when(userRepository.findById(uid)).thenReturn(Optional.of(dbUser));

        assertThatThrownBy(() -> userService.changePassword(uid, "wrongOld", "newPwd"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Current password is incorrect");

        verify(userRepository, times(1)).findById(uid);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword(): when old password is correct, save new encrypted password")
    void testChangePassword_Success() throws NoSuchFieldException, IllegalAccessException {
        Long uid = 12L;
        String oldPwd = "oldPwd";
        String newPwd = "newPwd123";
        String encodedOld = encoder.encode(oldPwd);
        User dbUser = new User("leo", "l@example.com", encodedOld);
        Field idField = dbUser.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(dbUser, uid);

        when(userRepository.findById(uid)).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changePassword(uid, oldPwd, newPwd);

        verify(userRepository, times(1)).save(any(User.class));
        // After saving, password should match the new raw password when encoded
        assertThat(encoder.matches(newPwd, dbUser.getPassword())).isTrue();
    }
}
