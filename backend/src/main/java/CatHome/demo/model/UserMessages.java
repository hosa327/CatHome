package CatHome.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_messages")
public class UserMessages {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "subscriptions", columnDefinition = "JSON")
    private String subscriptions;

    protected UserMessages() {}

    public UserMessages(Long userId) {
        this.userId = userId;
        this.subscriptions = "{}";
    }

    public Long getUserId() {
        return userId;
    }

    public String getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(String subscriptions) {
        this.subscriptions = subscriptions;
    }
}
