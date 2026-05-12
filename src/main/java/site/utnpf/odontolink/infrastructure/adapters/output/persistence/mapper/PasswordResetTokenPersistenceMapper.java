package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.PasswordResetToken;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PasswordResetTokenEntity;

/**
 * Mapper entre el modelo de dominio {@link PasswordResetToken} y la entidad
 * JPA {@link PasswordResetTokenEntity}. Mantener esta traducción explícita
 * preserva la independencia del dominio respecto a anotaciones JPA y permite
 * que el modelo del dominio evolucione sin acoplarse al esquema relacional.
 */
public class PasswordResetTokenPersistenceMapper {

    private PasswordResetTokenPersistenceMapper() {
        // Clase utilitaria, no instanciable.
    }

    public static PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        PasswordResetToken token = new PasswordResetToken();
        token.setId(entity.getId());
        token.setUserId(entity.getUserId());
        token.setTokenHash(entity.getTokenHash());
        token.setExpiresAt(entity.getExpiresAt());
        token.setUsedAt(entity.getUsedAt());
        token.setCreatedAt(entity.getCreatedAt());
        return token;
    }

    public static PasswordResetTokenEntity toEntity(PasswordResetToken token) {
        if (token == null) {
            return null;
        }
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setId(token.getId());
        entity.setUserId(token.getUserId());
        entity.setTokenHash(token.getTokenHash());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setUsedAt(token.getUsedAt());
        entity.setCreatedAt(token.getCreatedAt());
        return entity;
    }
}
