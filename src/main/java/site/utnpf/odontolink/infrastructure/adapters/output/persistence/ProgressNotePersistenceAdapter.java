package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.ProgressNote;
import site.utnpf.odontolink.domain.repository.ProgressNoteRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ProgressNoteEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaProgressNoteRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.ProgressNotePersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para ProgressNote (Hexagonal Architecture).
 * Implementa la interfaz del dominio ProgressNoteRepository usando JPA.
 * Puerto de salida (Output Adapter).
 *
 * Este adaptador soporta RF11 - CU 4.2: Registrar Evolución.
 *
 * @author OdontoLink Team
 */
@Component
public class ProgressNotePersistenceAdapter implements ProgressNoteRepository {

    private final JpaProgressNoteRepository jpaProgressNoteRepository;

    public ProgressNotePersistenceAdapter(JpaProgressNoteRepository jpaProgressNoteRepository) {
        this.jpaProgressNoteRepository = jpaProgressNoteRepository;
    }

    @Override
    public ProgressNote save(ProgressNote progressNote) {
        ProgressNoteEntity entity = ProgressNotePersistenceMapper.toEntity(progressNote);
        ProgressNoteEntity savedEntity = jpaProgressNoteRepository.save(entity);
        return ProgressNotePersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ProgressNote> findById(Long id) {
        return jpaProgressNoteRepository.findById(id)
                .map(ProgressNotePersistenceMapper::toDomain);
    }

    @Override
    public List<ProgressNote> findByAttentionId(Long attentionId) {
        // Usa la query con JOIN FETCH para cargar las relaciones necesarias
        return jpaProgressNoteRepository.findByAttentionIdOrderByCreatedAtDesc(attentionId).stream()
                .map(ProgressNotePersistenceMapper::toDomainWithRelations)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProgressNote> findByAuthorId(Long authorId) {
        // Usa la query con JOIN FETCH para cargar las relaciones necesarias
        return jpaProgressNoteRepository.findByAuthorIdOrderByCreatedAtDesc(authorId).stream()
                .map(ProgressNotePersistenceMapper::toDomainWithRelations)
                .collect(Collectors.toList());
    }
}
