package CatHome.demo.repository;

import CatHome.demo.model.Topic;
import CatHome.demo.model.UserMessages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    @Query("SELECT t.topicName FROM Topic t WHERE t.user.userId = :userId")
    Optional<List<String>>findTopicNamesByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Topic t WHERE t.user.userId = :userId AND t.topicName = :topicName")
    Optional<Topic> findTopicByUserIdAndTopicName(
            @Param("userId") Long userId,
            @Param("topicName") String topicName
    );
}
