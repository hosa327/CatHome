package CatHome.demo.repository;

import CatHome.demo.model.TopicMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicMessageRepository extends JpaRepository<TopicMessage, Long> {
    List<TopicMessage> findByTopic_TopicNameAndTopic_User_UserIdAndCatName(
            String topicName,
            Long userId,
            String catName
    );
}
