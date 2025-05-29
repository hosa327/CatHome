package CatHome.demo.model;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "topic_messages",
        indexes = @Index(name = "idx_topic_cat", columnList = "topic_id, cat_name"))
public class TopicMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "cat_name", nullable = false)
    private String catName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    protected TopicMessage() {}

    public TopicMessage(String catName, String payload, LocalDateTime receivedAt) {
        this.catName = catName;
        this.payload = payload;
        this.receivedAt = receivedAt;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Topic getTopic() {
        return topic;
    }

    void setTopic(Topic topic) {
        this.topic = topic;
    }

    public String getCatName() {
        return catName;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
}
