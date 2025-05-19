package CatHome.demo.repository;

import CatHome.demo.model.UserMessages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

@RepositoryRestResource
public interface IoTMessageRepository extends JpaRepository<UserMessages, Long> {

    @Query("SELECT u.subscriptions FROM UserMessages u WHERE u.userId = :userId")
    String findSubscriptions(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserMessages u " +
            "SET u.subscriptions = :json " +
            "WHERE u.userId = :userId")
    void updateSubscriptions(@Param("userId") Long userId,
                             @Param("json") String json);

    @Modifying
    @Transactional
    @Query(value =
            "UPDATE user_messages " +
                    "SET subscriptions = JSON_SET(subscriptions, " +
                    "    CONCAT('$.\"', :topic, '\"'), JSON_ARRAY()) " +
                    "WHERE user_id = :userId",
            nativeQuery = true)
    void addTopicKey(@Param("userId") Long userId,
                     @Param("topic") String topic);


    @Modifying
    @Transactional
    @Query(value =
            "UPDATE user_messages " +
                    "SET subscriptions = JSON_ARRAY_APPEND( " +
                    "    subscriptions, CONCAT('$.\"', :topic, '\"'), " +
                    "    JSON_OBJECT('payload', :payload, 'receivedAt', :receivedAt)" +
                    ") WHERE user_id = :userId",
            nativeQuery = true)
    void appendMessageWithTimestamp(@Param("userId") Long userId,
                                    @Param("topic") String topic,
                                    @Param("payload") String payload,
                                    @Param("receivedAt") String receivedAt);
}
