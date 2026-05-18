package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatbotSessionEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaChatbotSessionRepository extends JpaRepository<ChatbotSessionEntity, UUID> {

    Optional<ChatbotSessionEntity> findByAnonymousToken(UUID anonymousToken);

    @Modifying
    @Query("DELETE FROM ChatbotSessionEntity s WHERE s.lastInteractionAt < :cutoff")
    int deleteIdleOlderThan(@Param("cutoff") Instant cutoff);
}
