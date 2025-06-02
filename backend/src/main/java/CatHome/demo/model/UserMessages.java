package CatHome.demo.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "user_messages")
public class UserMessages {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Topic> topics = new ArrayList<>();

    protected UserMessages() {}

    public UserMessages(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
    public List<Topic> getTopics() { return topics; }

    public void addTopic(Topic topic) {
        topics.add(topic);
        topic.setUser(this);
    }

    public void removeTopic(Topic topic) {
        topics.remove(topic);
        topic.setUser(this);
    }
}
