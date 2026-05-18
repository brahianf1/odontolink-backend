package site.utnpf.odontolink.domain.model;

import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;

/**
 * Termino del diccionario de emergencias del chatbot (RF32).
 *
 * <p>El detector local pre-envio compara cada mensaje del usuario con la lista
 * de keywords activos. Si hay match, el use case marca {@code emergencyDetected}
 * en la respuesta y antepone un banner de derivacion (numero de emergencia /
 * guardia odontologica). Es defensa en profundidad ante el guardrail clinico
 * del proveedor: aunque el LLM falle en respetarlo, el FE recibe una flag
 * estructurada.
 *
 * <p>El {@code term} se normaliza al guardar (lowercase + sin acentos) para
 * que el detector funcione con cualquier variante que escriba el usuario
 * ("sangrado", "SANGRADO", "Sangrado"). El termino se sigue mostrando al admin
 * tal como se ingreso (no normalizado) para que conserve el formato legible.
 */
public class EmergencyKeyword {

    private Long id;
    private String term;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public EmergencyKeyword() {
    }

    public EmergencyKeyword(Long id, String term, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.term = term;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EmergencyKeyword createNew(String term, boolean active) {
        validateTerm(term);
        Instant now = Instant.now();
        return new EmergencyKeyword(null, term.trim(), active, now, now);
    }

    public void apply(String term, boolean active) {
        validateTerm(term);
        this.term = term.trim();
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            this.updatedAt = Instant.now();
        }
    }

    /**
     * Devuelve el termino normalizado para matching: lowercase y sin acentos.
     * Se computa cada vez en lugar de almacenarse porque el costo es trivial
     * y evita mantener un segundo campo duplicado.
     */
    public String getNormalizedTerm() {
        return normalize(this.term);
    }

    /**
     * Helper estatico de normalizacion (NFD + strip de diacriticos + lowercase).
     * Se expone para que el detector pueda normalizar tambien el input del
     * usuario con la misma logica.
     */
    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        return nfd.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.ROOT);
    }

    private static void validateTerm(String term) {
        if (term == null || term.isBlank()) {
            throw new InvalidBusinessRuleException("El campo 'term' es obligatorio.");
        }
        if (term.length() > 100) {
            throw new InvalidBusinessRuleException("El campo 'term' no puede exceder 100 caracteres.");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
