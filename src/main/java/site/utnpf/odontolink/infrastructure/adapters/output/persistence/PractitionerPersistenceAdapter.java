package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaPractitionerRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.PractitionerPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para Practitioner (Hexagonal Architecture).
 * Implementa la interfaz del dominio PractitionerRepository usando JPA.
 * Puerto de salida (Output Adapter).
 *
 * El adapter es {@code @Transactional(readOnly = true)} a nivel de clase para
 * garantizar que el mapeo entidad-a-dominio (que toca asociaciones LAZY como
 * {@code PractitionerEntity#user}) ocurra siempre dentro de la misma sesion
 * de Hibernate que ejecuto la query, incluso cuando el llamador (p.ej. el
 * AuthenticationFacade invocado desde un controller) no abrio una transaccion
 * propia. Sin esto, deshabilitar Open Session In View deja el Persistence
 * Context cerrado en el momento del mapeo y dispara LazyInitializationException.
 * Las operaciones de escritura sobreescriben con {@code @Transactional}
 * (sin readOnly) para no inhibir el flush de Hibernate.
 */
@Component
@Transactional(readOnly = true)
public class PractitionerPersistenceAdapter implements PractitionerRepository {

    private final JpaPractitionerRepository jpaPractitionerRepository;

    public PractitionerPersistenceAdapter(JpaPractitionerRepository jpaPractitionerRepository) {
        this.jpaPractitionerRepository = jpaPractitionerRepository;
    }

    @Override
    @Transactional
    public Practitioner save(Practitioner practitioner) {
        PractitionerEntity entity = PractitionerPersistenceMapper.toEntity(practitioner);
        PractitionerEntity savedEntity = jpaPractitionerRepository.save(entity);
        return PractitionerPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Practitioner> findById(Long id) {
        return jpaPractitionerRepository.findById(id)
                .map(PractitionerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Practitioner> findByUserId(Long userId) {
        return jpaPractitionerRepository.findByUserId(userId)
                .map(PractitionerPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByStudentId(String studentId) {
        return jpaPractitionerRepository.existsByStudentId(studentId);
    }

    @Override
    public List<Practitioner> searchByQuery(String query) {
        return jpaPractitionerRepository.searchByQuery(query)
                .stream()
                .map(PractitionerPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Practitioner> findAll() {
        return jpaPractitionerRepository.findAll()
                .stream()
                .map(PractitionerPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
