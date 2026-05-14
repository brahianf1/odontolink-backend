package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.PasswordResetToken;
import site.utnpf.odontolink.domain.repository.PasswordResetTokenRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PasswordResetTokenEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaPasswordResetTokenRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.PasswordResetTokenPersistenceMapper;

import java.time.Instant;
import java.util.Optional;

/**
 * Adaptador de salida que implementa {@link PasswordResetTokenRepository}
 * delegando en Spring Data JPA. Reside en la capa de infraestructura y
 * mantiene al dominio libre de cualquier dependencia técnica de persistencia.
 *
 * Politica transaccional uniforme con el resto de adapters; ver
 * {@link UserPersistenceAdapter} para el racional.
 */
@Component
@Transactional(readOnly = true)
public class PasswordResetTokenPersistenceAdapter implements PasswordResetTokenRepository {

    private final JpaPasswordResetTokenRepository jpaRepository;

    public PasswordResetTokenPersistenceAdapter(JpaPasswordResetTokenRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenEntity entity = PasswordResetTokenPersistenceMapper.toEntity(token);
        PasswordResetTokenEntity saved = jpaRepository.save(entity);
        return PasswordResetTokenPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(PasswordResetTokenPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public void invalidateActiveTokensForUser(Long userId, Instant invalidatedAt) {
        jpaRepository.invalidateActiveTokensForUser(userId, invalidatedAt);
    }
}
