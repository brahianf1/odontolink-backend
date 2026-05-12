package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.PasswordResetToken;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia de tokens de recuperación de contraseña.
 * Pertenece al dominio y será implementado por un adaptador en la capa de
 * infraestructura (JPA). El contrato expone únicamente las operaciones
 * realmente requeridas por el caso de uso, manteniéndolo intencionalmente
 * estrecho para reforzar el principio de inversión de dependencias.
 */
public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    /**
     * Busca un token activo (no consumido y no expirado se valida en dominio)
     * a partir del hash almacenado. El consumidor recibe el hash y nunca el
     * valor en claro, garantizando que la BD jamás expone tokens utilizables
     * en caso de compromiso.
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Invalida (marca como consumidos en el instante recibido) todos los
     * tokens previos aún activos del usuario indicado. Se utiliza al emitir
     * un nuevo token para evitar coexistencia de varios tokens válidos
     * simultáneos asociados a la misma cuenta.
     */
    void invalidateActiveTokensForUser(Long userId, java.time.Instant invalidatedAt);
}
