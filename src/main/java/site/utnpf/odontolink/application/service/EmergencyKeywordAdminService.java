package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IEmergencyKeywordAdminUseCase;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.EmergencyKeyword;
import site.utnpf.odontolink.domain.repository.EmergencyKeywordRepository;

import java.util.List;

/**
 * Servicio CRUD del diccionario de emergencias del chatbot (RF32).
 *
 * <p>Mantiene la regla de unicidad case-insensitive normalizada: dos terminos
 * que normalizados (sin acentos, lowercase) coinciden son considerados
 * duplicados y se rechazan con 422.
 */
@Transactional
public class EmergencyKeywordAdminService implements IEmergencyKeywordAdminUseCase {

    private final EmergencyKeywordRepository repository;

    public EmergencyKeywordAdminService(EmergencyKeywordRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyKeyword> listAll() {
        return repository.findAllOrderByTermAsc();
    }

    @Override
    public EmergencyKeyword create(String term, boolean active) {
        rejectIfDuplicate(term, null);
        EmergencyKeyword kw = EmergencyKeyword.createNew(term, active);
        return repository.save(kw);
    }

    @Override
    public EmergencyKeyword update(Long id, String term, boolean active) {
        EmergencyKeyword existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EmergencyKeyword", "id", String.valueOf(id)));
        rejectIfDuplicate(term, id);
        existing.apply(term, active);
        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EmergencyKeyword", "id", String.valueOf(id)));
        repository.deleteById(id);
    }

    /**
     * Rechaza si ya existe otra keyword con el mismo termino normalizado.
     * Cuando {@code excludeId != null} permite editar la misma fila sin
     * disparar el conflicto consigo misma.
     *
     * <p>La unicidad es <strong>normalizada</strong> en el dominio (lowercase
     * + sin acentos via {@link EmergencyKeyword#normalize}), no solo
     * case-insensitive en SQL: dos terminos como {@code "Infección"} e
     * {@code "infeccion"} se consideran duplicados, aunque el collation de
     * MySQL los compare distinto. Comparamos en memoria contra
     * {@code findAllOrderByTermAsc()} porque la lista es chica (<= ~30 en
     * produccion) y nos garantiza el resultado correcto independiente del
     * collation de la BD.
     */
    private void rejectIfDuplicate(String term, Long excludeId) {
        if (term == null || term.isBlank()) {
            // El dominio rechaza esto con su propia validacion; pero
            // anticipamos para no pegar a BD con un texto invalido.
            throw new InvalidBusinessRuleException("El campo 'term' es obligatorio.");
        }
        String normalized = EmergencyKeyword.normalize(term);
        repository.findAllOrderByTermAsc().stream()
                .filter(kw -> kw.getNormalizedTerm().equals(normalized))
                .findFirst()
                .ifPresent(existing -> {
                    if (excludeId == null || !excludeId.equals(existing.getId())) {
                        throw new InvalidBusinessRuleException(
                                "Ya existe una keyword de emergencia con ese termino.");
                    }
                });
    }
}
