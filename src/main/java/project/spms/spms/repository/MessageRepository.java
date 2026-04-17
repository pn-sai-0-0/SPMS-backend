package project.spms.spms.repository;

import project.spms.spms.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByToIdOrderByCreatedAtDesc(Integer toId);

    List<Message> findByFromIdOrderByCreatedAtDesc(Integer fromId);

    Long countByToIdAndIsReadFalse(Integer toId);

    // Bug Fix #6: fetch full conversation between two users (both directions)
    @Query("SELECT m FROM Message m WHERE (m.fromId = :userId AND m.toId = :partnerId) OR (m.fromId = :partnerId AND m.toId = :userId) ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("userId") Integer userId, @Param("partnerId") Integer partnerId);

    // Bug Fix #6: mark a single message as read
    @Modifying
    @Query(value = "UPDATE messages SET is_read = 1 WHERE id = :id", nativeQuery = true)
    void markAsRead(@Param("id") Integer id);

    // Bug Fix #6: mark all messages from a sender to a recipient as read
    @Modifying
    @Query(value = "UPDATE messages SET is_read = 1 WHERE to_id = :toId AND from_id = :fromId AND is_read = 0", nativeQuery = true)
    void markAllReadInConversation(@Param("toId") Integer toId, @Param("fromId") Integer fromId);
}
