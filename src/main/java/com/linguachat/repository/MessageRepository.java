package com.linguachat.repository;

import com.linguachat.entity.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("""
            select m from Message m
            where (m.sender.id = :userA and m.receiver.id = :userB)
               or (m.sender.id = :userB and m.receiver.id = :userA)
            order by m.createdAt asc
            """)
    List<Message> findConversation(@Param("userA") Long userA, @Param("userB") Long userB);

    @Modifying
    @Query(value = """
            update messages set status = :status, updated_at = now()
            where sender_id = :senderId and receiver_id = :receiverId and status <> 'SEEN'
            """, nativeQuery = true)
    int updateDeliveredForConversation(@Param("senderId") Long senderId,
                                       @Param("receiverId") Long receiverId,
                                       @Param("status") String status);

    @Modifying
    @Query(value = """
            update messages set status = 'SEEN', updated_at = now()
            where sender_id = :senderId and receiver_id = :receiverId
            """, nativeQuery = true)
    int markSeen(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    @Modifying
    @Query(value = """
            update messages
            set deleted = true,
                original_text = 'This chat was cleared',
                translated_text = 'This chat was cleared',
                attachment_name = null,
                attachment_data = null,
                updated_at = now()
            where (sender_id = :userA and receiver_id = :userB)
               or (sender_id = :userB and receiver_id = :userA)
            """, nativeQuery = true)
    int clearConversation(@Param("userA") Long userA, @Param("userB") Long userB);

    @Modifying
    @Query(value = "delete from messages where sender_id = :userId or receiver_id = :userId", nativeQuery = true)
    int deleteMessagesInvolvingUser(@Param("userId") Long userId);
}
