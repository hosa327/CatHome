package CatHome.demo.dto;
import CatHome.demo.model.User;

public class UserInfo {
    private Long id;
    private String email;
    private String name;

    private String avatarRelativePath;

    public UserInfo(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getUserName();
        this.avatarRelativePath = user.getAvatarRelativePath();
    }

    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getAvatarRelativePath(){
        return avatarRelativePath;
    }
}
