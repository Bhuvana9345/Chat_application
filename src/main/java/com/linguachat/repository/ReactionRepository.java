package com.linguachat.repository;

import com.linguachat.entity.Reaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByMessageId(Long messageId);
    List<Reaction> findByMessageIdIn(List<Long> messageIds);
    Optional<Reaction> findByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);

    @Modifying
    @Query(value = "delete from reactions where user_id = :userId", nativeQuery = true)
    int deleteByUserIdNative(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            delete from reactions
            where message_id in (
                select id from messages where sender_id = :userId or receiver_id = :userId
            )
            """, nativeQuery = true)
    int deleteForMessagesInvolvingUser(@Param("userId") Long userId);
}
