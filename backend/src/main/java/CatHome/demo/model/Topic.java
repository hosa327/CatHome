package CatHome.demo.model;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "topics",
        indexes = @Index(name = "idx_user_topic", columnList = "user_id, topic_name"))
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    private Long topicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserMessages user;

    @Column(name = "topic_name", nullable = false)
    private String topicName;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TopicMessage> messages = new ArrayList<>();

    protected Topic() {}

    public Topic(String topicName) {
        this.topicName = topicName;
    }

    public Long getTopicId() {
        return topicId;
    }

    public UserMessages getUser() {
        return user;
    }

    void setUser(UserMessages user) {
        this.user = user;
    }

    public String getTopicName() {
        return topicName;
    }

    public List<TopicMessage> getMessages() {
        return messages;
    }

    public void addMessage(TopicMessage msg) {
        messages.add(msg);
        msg.setTopic(this);
    }

    public void removeMessage(TopicMessage msg) {
        messages.remove(msg);
        msg.setTopic(null);
    }
}
