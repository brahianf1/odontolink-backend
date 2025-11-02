package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.ProgressNote;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para ProgressNote (Nota de Progreso/Evolución).
 * Puerto de salida (Output Port) en arquitectura hexagonal.
 */
public interface ProgressNoteRepository {

    /**
     * Guarda una nueva nota de progreso o actualiza una existente.
     */
    ProgressNote save(ProgressNote progressNote);

    /**
     * Busca una nota de progreso por su ID.
     */
    Optional<ProgressNote> findById(Long id);

    /**
     * Obtiene todas las notas de progreso de una atención específica.
     * Ordenadas por fecha de creación (más recientes primero).
     *
     * @param attentionId ID de la atención (caso clínico)
     * @return Lista de notas de progreso
     */
    List<ProgressNote> findByAttentionId(Long attentionId);

    /**
     * Obtiene todas las notas de progreso de un autor específico.
     *
     * @param authorId ID del usuario autor
     * @return Lista de notas de progreso
     */
    List<ProgressNote> findByAuthorId(Long authorId);
}
