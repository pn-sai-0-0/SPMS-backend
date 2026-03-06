package project.spms.spms.repository;

import project.spms.spms.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByToIdOrderByCreatedAtDesc(Integer toId);

    List<Message> findByFromIdOrderByCreatedAtDesc(Integer fromId);

    Long countByToIdAndIsReadFalse(Integer toId);
}