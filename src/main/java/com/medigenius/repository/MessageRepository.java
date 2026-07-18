package com.medigenius.repository;

import com.medigenius.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Replaces the raw-SQL/SQLAlchemy queries in backend/app/services/database_service.py.
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Equivalent of database_service.get_session_history():
     * "SELECT * FROM messages WHERE session_id = ? ORDER BY timestamp ASC"
     */
    List<Message> findBySessionIdOrderByTimestampAsc(String sessionId);

    /**
     * NEW (Feature 4/12) - all messages for a given user, used by DatabaseService /
     * profile stats. Additive; does not change any existing query.
     */
    List<Message> findByUserIdOrderByTimestampAsc(Long userId);

    /**
     * Equivalent of database_service.delete_session():
     * "DELETE FROM messages WHERE session_id = ?"
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.sessionId = :sessionId")
    void deleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * Equivalent of database_service.get_all_sessions(): one row per distinct session_id,
     * showing the most recent message's content as the preview and its timestamp,
     * ordered by most-recently-active session first.
     *
     * Mirrors the Python "group by session_id, take latest row" query using a
     * correlated subquery for portability across MySQL versions.
     */
    @Query(value = """
            SELECT m.session_id AS sessionId,
                   m.content    AS preview,
                   m.timestamp  AS lastActive
            FROM messages m
            INNER JOIN (
                SELECT session_id, MAX(timestamp) AS max_ts
                FROM messages
                WHERE role = 'user'
                GROUP BY session_id
            ) latest ON m.session_id = latest.session_id AND m.timestamp = latest.max_ts
            WHERE m.role = 'user'
            ORDER BY m.timestamp DESC
            """, nativeQuery = true)
    List<SessionPreviewProjection> findAllSessionPreviews();

    /**
     * Projection interface for the native aggregate query above.
     */
    interface SessionPreviewProjection {
        String getSessionId();
        String getPreview();
        java.sql.Timestamp getLastActive();
    }
}
