package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.EmergencyKeyword;
import site.utnpf.odontolink.domain.repository.EmergencyKeywordRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.EmergencyKeywordEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaEmergencyKeywordRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.EmergencyKeywordPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Adaptador de persistencia para {@link EmergencyKeyword} (RF32).
 *
 * <p>Incluye un cache local en memoria con TTL de 60s para la lista de
 * keywords activos: el detector lo consulta en cada turno del chatbot y no
 * tiene sentido pegar a BD. El TTL es corto deliberadamente: cambios del
 * admin propagan en hasta 60s sin necesidad de eventing.
 */
@Component
@Transactional(readOnly = true)
public class EmergencyKeywordPersistenceAdapter implements EmergencyKeywordRepository {

    private static final long CACHE_TTL_MS = 60_000L;

    private final JpaEmergencyKeywordRepository jpa;

    /** Snapshot atomico: (timestamp, lista). Reemplazo entero al refrescar. */
    private final AtomicReference<CacheSnapshot> activeCache = new AtomicReference<>(
            new CacheSnapshot(0L, List.of()));

    public EmergencyKeywordPersistenceAdapter(JpaEmergencyKeywordRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<EmergencyKeyword> findAllOrderByTermAsc() {
        return jpa.findAllByOrderByTermAsc().stream()
                .map(EmergencyKeywordPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<EmergencyKeyword> findAllActive() {
        long now = System.currentTimeMillis();
        CacheSnapshot snap = activeCache.get();
        if (now - snap.timestamp() < CACHE_TTL_MS) {
            return snap.value();
        }
        List<EmergencyKeyword> fresh = jpa.findAllByActiveTrueOrderByTermAsc().stream()
                .map(EmergencyKeywordPersistenceMapper::toDomain)
                .toList();
        activeCache.set(new CacheSnapshot(now, fresh));
        return fresh;
    }

    @Override
    public Optional<EmergencyKeyword> findById(Long id) {
        return jpa.findById(id).map(EmergencyKeywordPersistenceMapper::toDomain);
    }

    @Override
    public Optional<EmergencyKeyword> findByTermIgnoreCase(String term) {
        return jpa.findFirstByTermIgnoreCase(term)
                .map(EmergencyKeywordPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public EmergencyKeyword save(EmergencyKeyword keyword) {
        EmergencyKeywordEntity entity = EmergencyKeywordPersistenceMapper.toEntity(keyword);
        EmergencyKeywordEntity saved = jpa.save(entity);
        invalidateCache();
        return EmergencyKeywordPersistenceMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpa.deleteById(id);
        invalidateCache();
    }

    private void invalidateCache() {
        activeCache.set(new CacheSnapshot(0L, List.of()));
    }

    private record CacheSnapshot(long timestamp, List<EmergencyKeyword> value) {
    }
}
