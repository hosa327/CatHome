package CatHome.demo.model;

import java.io.Serializable;
import java.util.Objects;

public class LatestDataMessageId implements Serializable {
    private Long userId;
    private String catName;

    public LatestDataMessageId() {}
    public LatestDataMessageId(Long userId, String catName) {
        this.userId = userId;
        this.catName = catName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LatestDataMessageId)) return false;
        LatestDataMessageId that = (LatestDataMessageId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(catName, that.catName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, catName);
    }
}

