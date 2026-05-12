package site.utnpf.odontolink.domain.model;

import java.time.Instant;

/**
 * Modelo de dominio que representa un token de recuperación de contraseña (RF04).
 *
 * Encapsula las reglas de negocio relacionadas al ciclo de vida del token:
 * vigencia temporal, consumo único e invalidación. Sólo el hash del token
 * vive en este modelo (y en la persistencia) para limitar el daño en caso
 * de filtración de la base de datos: el valor en claro únicamente existe
 * en memoria al momento de emitirlo y se entrega al usuario por correo.
 */
public class PasswordResetToken {

    private Long id;
    private Long userId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant createdAt;

    public PasswordResetToken() {
    }

    /**
     * Constructor utilizado al emitir un nuevo token. Inicializa los campos
     * de auditoría temporal y deja el campo {@code usedAt} en null hasta
     * que el token sea efectivamente consumido.
     */
    public PasswordResetToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
        this.usedAt = null;
    }

    /**
     * Determina si el token sigue siendo utilizable.
     * Se separa esta lógica en un método para que el servicio de aplicación
     * no necesite conocer la representación interna del estado del token.
     */
    public boolean isUsable(Instant now) {
        return !isExpired(now) && !isUsed();
    }

    /**
     * Un token se considera expirado cuando el instante actual es posterior
     * o igual a su {@code expiresAt}. La comparación inclusiva en el límite
     * superior protege contra condiciones de carrera en los bordes.
     */
    public boolean isExpired(Instant now) {
        return now.compareTo(expiresAt) >= 0;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Marca el token como consumido. Forzar el paso por este método garantiza
     * que sólo el dominio decide cuándo y cómo se invalida un token, evitando
     * que adaptadores externos manipulen el estado directamente.
     */
    public void markAsUsed(Instant now) {
        if (isUsed()) {
            throw new IllegalStateException("El token ya fue utilizado.");
        }
        this.usedAt = now;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
