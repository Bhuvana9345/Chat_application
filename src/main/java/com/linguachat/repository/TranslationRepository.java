package com.linguachat.repository;

import com.linguachat.entity.Translation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TranslationRepository extends JpaRepository<Translation, Long> {
    Optional<Translation> findByMessageId(Long messageId);

    @Modifying
    @Query(value = """
            delete from translations
            where message_id in (
                select id from messages where sender_id = :userId or receiver_id = :userId
            )
            """, nativeQuery = true)
    int deleteForMessagesInvolvingUser(@Param("userId") Long userId);
}
