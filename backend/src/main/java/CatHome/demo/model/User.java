package CatHome.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 64)
    private String password;

    @Lob
    @Column(name = "cert_pem", columnDefinition = "TEXT", nullable = true)
    private String certPem;

    @Lob
    @Column(name = "private_key_pem", columnDefinition = "TEXT", nullable = true)
    private String privateKeyPem;

    @Lob
    @Column(name = "ca_pem", columnDefinition = "TEXT", nullable = true)
    private String caPem;

    @Lob
    @Column(name = "end_point", columnDefinition = "TEXT", nullable = true)
    private String end_point;

    @Lob
    @Column(name = "clientId", columnDefinition = "TEXT", nullable = true)
    private String clientId;

    @Column(nullable = true, length = 255)
    private String avatarRelativePath;

    public User(Long id) {
        this.id = id;
    }

    public User() {
    }
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }


    public Long getId() {
        return id;
    }
    public String getPassword() {
        return password;
    }

    public String getUserName(){
        return username;
    }

    public String getEmail(){
        return email;
    }
    public void setAvatarRelativePath(String avatarRelativePath){
        this.avatarRelativePath = avatarRelativePath;
    }
    public String getAvatarRelativePath(){
        return avatarRelativePath;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getCertPem() {
        return certPem;
    }
    public void setCertPem(String certPem) {
        this.certPem = certPem;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }
    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    public String getCaPem() {
        return caPem;
    }
    public void setCaPem(String caPem) {
        this.caPem = caPem;
    }

    public String getEnd_point() {
        return end_point;
    }
    public void setEnd_point(String end_point) {
        this.caPem = end_point;
    }

    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
