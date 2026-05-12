package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PasswordResetTokenEntity;

import java.time.Instant;
import java.util.Optional;

/**
 * Repositorio Spring Data para {@link PasswordResetTokenEntity}.
 *
 * Vive en la capa de infraestructura y se mantiene aislado del dominio mediante
 * el adaptador que implementa el puerto de salida. Aquí concentramos las
 * consultas en JPQL para mantenerlas explícitas y revisables, evitando
 * derivaciones implícitas que dificultan auditoría de seguridad.
 */
@Repository
public interface JpaPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    /**
     * Marca como consumidos todos los tokens activos (no expirados y no usados)
     * pertenecientes al usuario. Se ejecuta en una sola sentencia para evitar
     * cargar entidades innecesarias y reducir presión sobre la caché de Hibernate.
     */
    @Modifying
    @Query("""
            UPDATE PasswordResetTokenEntity t
               SET t.usedAt = :invalidatedAt
             WHERE t.userId = :userId
               AND t.usedAt IS NULL
               AND t.expiresAt > :invalidatedAt
            """)
    int invalidateActiveTokensForUser(@Param("userId") Long userId,
                                      @Param("invalidatedAt") Instant invalidatedAt);
}
