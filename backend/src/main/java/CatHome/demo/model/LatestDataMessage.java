package CatHome.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "latest_data_messages")
@IdClass(LatestDataMessageId.class)
public class LatestDataMessage {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "cat_name")
    private String catName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;


    protected LatestDataMessage() {}

    public LatestDataMessage(Long userId, String catName) {
        this.userId = userId;
        this.catName = catName;
        this.payload = "{\"catName\":\"" + catName + "\"}";
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getPayload(){
        return payload;
    }
}
