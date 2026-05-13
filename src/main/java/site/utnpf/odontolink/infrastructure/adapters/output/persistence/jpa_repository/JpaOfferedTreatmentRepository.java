package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.OfferedTreatmentEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.TreatmentEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para OfferedTreatmentEntity.
 * Proporciona operaciones CRUD, búsquedas convencionales y composición
 * dinámica de queries vía Specifications (motor del catálogo público).
 *
 * Hereda de {@link JpaSpecificationExecutor} para habilitar la API de
 * Criteria desde {@code OfferedTreatmentSpecifications}, que es el
 * mecanismo elegido para RF09 por permitir filtros opcionales combinables
 * sin caer en JPQL dinámico difícil de mantener.
 */
public interface JpaOfferedTreatmentRepository
        extends JpaRepository<OfferedTreatmentEntity, Long>,
                JpaSpecificationExecutor<OfferedTreatmentEntity> {

    /**
     * Obtiene todas las ofertas de tratamientos de un practicante.
     */
    List<OfferedTreatmentEntity> findByPractitioner(PractitionerEntity practitioner);

    /**
     * Obtiene todas las ofertas de tratamientos de un practicante por su ID.
     */
    List<OfferedTreatmentEntity> findByPractitionerId(Long practitionerId);

    /**
     * Obtiene las ofertas ACTIVAS filtradas por tipo de tratamiento.
     * Endpoint legacy: el catálogo público nunca debe ver bajas lógicas.
     */
    List<OfferedTreatmentEntity> findByTreatmentIdAndActiveTrue(Long treatmentId);

    /**
     * Obtiene todas las ofertas ACTIVAS (catálogo público sin filtros).
     */
    List<OfferedTreatmentEntity> findByActiveTrue();

    /**
     * Verifica unicidad sobre el catálogo VIGENTE del practicante.
     *
     * Sólo cuentan las ofertas activas: si una se dio de baja lógica por
     * RF16 queda como histórico y no debe impedir agregar una nueva con
     * el mismo treatment.
     */
    boolean existsByPractitionerAndTreatmentAndActiveTrue(
            PractitionerEntity practitioner,
            TreatmentEntity treatment
    );

    /**
     * Busca una oferta específica (activa o no) por practitioner + treatment.
     * Útil para flujos administrativos que necesitan resolver el registro
     * histórico independientemente de su estado.
     */
    Optional<OfferedTreatmentEntity> findByPractitionerAndTreatment(
            PractitionerEntity practitioner,
            TreatmentEntity treatment
    );

    /**
     * Verifica si existen turnos SCHEDULED a futuro asociados al par
     * practitioner+treatment de la oferta dada.
     *
     * Razón del diseño: la FK no está en Appointment hacia OfferedTreatment;
     * la asociación se reconstruye en SQL transitando por
     * Appointment → Attention → (practitioner, treatment) y comparando contra
     * la oferta del parámetro. Esto es lo que permite reutilizar el modelo
     * actual sin migrar el esquema de Appointments.
     *
     * @param offeredTreatmentId ID de la oferta a evaluar
     * @param now Fecha/hora de corte ("futuro" estricto)
     * @return true si existe al menos un SCHEDULED ≥ now
     */
    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM AppointmentEntity a
            WHERE a.attention.practitioner.id = (
                SELECT ot.practitioner.id FROM OfferedTreatmentEntity ot WHERE ot.id = :offeredTreatmentId
            )
            AND a.attention.treatment.id = (
                SELECT ot.treatment.id FROM OfferedTreatmentEntity ot WHERE ot.id = :offeredTreatmentId
            )
            AND a.status = site.utnpf.odontolink.domain.model.AppointmentStatus.SCHEDULED
            AND a.appointmentTime >= :now
            """)
    boolean hasFutureScheduledAppointments(
            @Param("offeredTreatmentId") Long offeredTreatmentId,
            @Param("now") LocalDateTime now
    );

    /**
     * Verifica si existen Atenciones IN_PROGRESS para el par
     * practitioner+treatment de la oferta.
     */
    @Query("""
            SELECT CASE WHEN COUNT(att) > 0 THEN true ELSE false END
            FROM AttentionEntity att
            WHERE att.practitioner.id = (
                SELECT ot.practitioner.id FROM OfferedTreatmentEntity ot WHERE ot.id = :offeredTreatmentId
            )
            AND att.treatment.id = (
                SELECT ot.treatment.id FROM OfferedTreatmentEntity ot WHERE ot.id = :offeredTreatmentId
            )
            AND att.status = site.utnpf.odontolink.domain.model.AttentionStatus.IN_PROGRESS
            """)
    boolean hasInProgressAttentions(@Param("offeredTreatmentId") Long offeredTreatmentId);

    /**
     * Verifica si existen Atenciones de cualquier estado (incluye históricas)
     * para el par practitioner+treatment de la oferta.
     */
    @Query("""
            SELECT CASE WHEN COUNT(att) > 0 THEN true ELSE false END
            FROM AttentionEntity att
            WHERE att.practitioner.id = (
                SELECT ot.practitioner.id FROM OfferedTreatmentEntity ot WHERE ot.id = :offeredTreatmentId
            )
            AND att.treatment.id = (
                SELECT ot.treatment.id FROM OfferedTreatmentEntity ot WHERE ot.id = :offeredTreatmentId
            )
            """)
    boolean hasAnyAttentions(@Param("offeredTreatmentId") Long offeredTreatmentId);
}
