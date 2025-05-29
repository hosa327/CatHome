package CatHome.demo.repository;

import CatHome.demo.model.UserMessages;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMessageRepository extends JpaRepository<UserMessages, Long> {

    boolean existsByUserId(Long userId);

    long deleteByUserId(Long userId);


    Page<UserMessages> findAll(Pageable pageable);
}
