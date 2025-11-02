package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ProgressNoteEntity;

import java.util.List;

/**
 * Repositorio JPA para ProgressNoteEntity.
 * Extiende JpaRepository para obtener operaciones CRUD básicas.
 *
 * Este repositorio soporta RF11 - CU 4.2: Registrar Evolución.
 */
public interface JpaProgressNoteRepository extends JpaRepository<ProgressNoteEntity, Long> {

    /**
     * Obtiene todas las notas de progreso de una atención específica.
     * Ordenadas por fecha de creación descendente (más recientes primero).
     * Carga eager de las relaciones necesarias para evitar N+1.
     *
     * @param attentionId ID de la atención (caso clínico)
     * @return Lista de notas de progreso ordenadas por fecha descendente
     */
    @Query("SELECT pn FROM ProgressNoteEntity pn " +
           "JOIN FETCH pn.author " +
           "WHERE pn.attention.id = :attentionId " +
           "ORDER BY pn.createdAt DESC")
    List<ProgressNoteEntity> findByAttentionIdOrderByCreatedAtDesc(
            @Param("attentionId") Long attentionId
    );

    /**
     * Obtiene todas las notas de progreso de un autor específico.
     * Útil para ver el historial de notas escritas por un practicante o supervisor.
     *
     * @param authorId ID del usuario autor
     * @return Lista de notas de progreso del autor
     */
    @Query("SELECT pn FROM ProgressNoteEntity pn " +
           "JOIN FETCH pn.attention " +
           "WHERE pn.author.id = :authorId " +
           "ORDER BY pn.createdAt DESC")
    List<ProgressNoteEntity> findByAuthorIdOrderByCreatedAtDesc(
            @Param("authorId") Long authorId
    );
}
