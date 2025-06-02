package CatHome.demo.repository;

import CatHome.demo.model.TopicMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TopicMessageRepository extends JpaRepository<TopicMessage, Long> {
    @Query(
            value = """
        SELECT tm.*
          FROM topic_messages tm
          JOIN topics t 
            ON tm.topic_id = t.topic_id
         WHERE t.topic_name = :topicName
           AND t.user_id    = :userId
           AND tm.cat_name  = :catName
         ORDER BY tm.received_at ASC
        """,
            nativeQuery = true
    )
    List<TopicMessage> findByUserIdAndTopicNameAndCatName(
            @Param("userId") Long userId,
            @Param("topicName") String topicName,
            @Param("catName") String catName
    );
}
