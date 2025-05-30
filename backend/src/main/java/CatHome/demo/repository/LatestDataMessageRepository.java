package CatHome.demo.repository;

import CatHome.demo.model.LatestDataMessage;
import CatHome.demo.model.LatestDataMessageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface LatestDataMessageRepository
        extends JpaRepository<LatestDataMessage, LatestDataMessageId> {

    @Modifying
    @Transactional
    @Query(value = """
      INSERT INTO latest_data_messages(user_id, cat_name, payload)
      VALUES (:userId, :catName, :payload)
      ON DUPLICATE KEY UPDATE
        payload     = VALUES(payload)
      """, nativeQuery = true)
    void upsertLatest(
            @Param("userId") Long userId,
            @Param("catName") String catName,
            @Param("payload") String payload
    );

    @Query(value = """
      SELECT cat_name
      FROM latest_data_messages
      WHERE user_id = :userId
      """, nativeQuery = true)
    List<String> findCatNamesByUserId(@Param("userId") Long userId);


    @Query(value = """
      SELECT user_id,
             cat_name,
             payload
      FROM latest_data_messages
      WHERE user_id = :userId
        And cat_name = :catName
      """, nativeQuery = true)
    Optional<LatestDataMessage> findByUserIdAndCatName(@Param("userId") Long userId,
                                             @Param("catName") String catName);

}

