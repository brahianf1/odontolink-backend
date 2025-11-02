package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.ProgressNote;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ProgressNoteEntity;

/**
 * Mapper para convertir entre ProgressNote (dominio) y ProgressNoteEntity (persistencia).
 *
 * Este mapper sigue el patrón de evitar ciclos infinitos al mapear relaciones bidireccionales.
 * La Attention contiene la lista de ProgressNotes, por lo que no mapeamos la relación inversa
 * en la conversión básica.
 *
 * @author OdontoLink Team
 */
public class ProgressNotePersistenceMapper {

    private ProgressNotePersistenceMapper() {
        // Utility class
    }

    /**
     * Convierte de entidad JPA a modelo de dominio.
     * IMPORTANTE: No mapea 'attention' para evitar ciclos infinitos.
     * La Attention ya contiene la lista de ProgressNotes.
     *
     * @param entity Entidad JPA ProgressNoteEntity
     * @return Modelo de dominio ProgressNote
     */
    public static ProgressNote toDomain(ProgressNoteEntity entity) {
        if (entity == null) {
            return null;
        }

        ProgressNote progressNote = new ProgressNote();
        progressNote.setId(entity.getId());
        progressNote.setNote(entity.getNote());
        progressNote.setCreatedAt(entity.getCreatedAt());

        // Mapear el autor (User)
        if (entity.getAuthor() != null) {
            progressNote.setAuthor(UserPersistenceMapper.toDomain(entity.getAuthor()));
        }

        // NOTE: No mapeamos 'attention' aquí para evitar ciclos infinitos
        // La relación se establece desde AttentionPersistenceMapper

        return progressNote;
    }

    /**
     * Convierte de entidad JPA a modelo de dominio CON relaciones cargadas.
     * Usado cuando las relaciones ya fueron cargadas mediante JOIN FETCH.
     *
     * Este método es útil para consultas que cargan ProgressNote con su Attention completa.
     *
     * @param entity Entidad JPA con relaciones cargadas
     * @return Modelo de dominio ProgressNote con relaciones
     */
    public static ProgressNote toDomainWithRelations(ProgressNoteEntity entity) {
        if (entity == null) {
            return null;
        }

        ProgressNote progressNote = new ProgressNote();
        progressNote.setId(entity.getId());
        progressNote.setNote(entity.getNote());
        progressNote.setCreatedAt(entity.getCreatedAt());

        // Mapear el autor
        if (entity.getAuthor() != null) {
            progressNote.setAuthor(UserPersistenceMapper.toDomain(entity.getAuthor()));
        }

        // Mapear la Attention con versión "shallow" para evitar recursión infinita
        if (entity.getAttention() != null) {
            progressNote.setAttention(AttentionPersistenceMapper.toDomainShallow(entity.getAttention()));
        }

        return progressNote;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     * IMPORTANTE: No mapea 'attention' para evitar ciclos infinitos.
     * La relación se establece en AttentionPersistenceMapper.
     *
     * @param progressNote Modelo de dominio ProgressNote
     * @return Entidad JPA ProgressNoteEntity
     */
    public static ProgressNoteEntity toEntity(ProgressNote progressNote) {
        if (progressNote == null) {
            return null;
        }

        ProgressNoteEntity entity = new ProgressNoteEntity();
        entity.setId(progressNote.getId());
        entity.setNote(progressNote.getNote());
        entity.setCreatedAt(progressNote.getCreatedAt());

        // Mapear el autor
        if (progressNote.getAuthor() != null) {
            entity.setAuthor(UserPersistenceMapper.toEntity(progressNote.getAuthor()));
        }

        // NOTE: No mapeamos 'attention' aquí para evitar ciclos infinitos
        // La relación se establece desde AttentionPersistenceMapper cuando
        // se guarda la Attention completa con sus ProgressNotes

        return entity;
    }
}
