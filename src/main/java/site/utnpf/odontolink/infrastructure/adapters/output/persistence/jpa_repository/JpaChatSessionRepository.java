package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.ChatSessionEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PatientEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para ChatSessionEntity.
 * Extiende JpaRepository de Spring Data JPA para operaciones CRUD automáticas.
 *
 * Spring Data JPA genera automáticamente las implementaciones de estos métodos
 * basándose en sus nombres siguiendo las convenciones de nomenclatura.
 *
 * @author OdontoLink Team
 */
@Repository
public interface JpaChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {

    /**
     * Encuentra todas las sesiones de chat de un paciente.
     *
     * @param patient La entidad del paciente
     * @return Lista de sesiones del paciente
     */
    List<ChatSessionEntity> findByPatient(PatientEntity patient);

    /**
     * Encuentra todas las sesiones de chat de un practicante.
     *
     * @param practitioner La entidad del practicante
     * @return Lista de sesiones del practicante
     */
    List<ChatSessionEntity> findByPractitioner(PractitionerEntity practitioner);

    /**
     * Verifica si existe una sesión de chat entre un paciente y un practicante.
     *
     * @param patient La entidad del paciente
     * @param practitioner La entidad del practicante
     * @return true si existe, false en caso contrario
     */
    boolean existsByPatientAndPractitioner(PatientEntity patient, PractitionerEntity practitioner);

    /**
     * Encuentra la sesión de chat específica entre un paciente y un practicante.
     *
     * @param patient La entidad del paciente
     * @param practitioner La entidad del practicante
     * @return Optional con la sesión si existe
     */
    Optional<ChatSessionEntity> findByPatientAndPractitioner(PatientEntity patient, PractitionerEntity practitioner);

    /**
     * Encuentra todas las sesiones de chat de un paciente por su ID.
     *
     * @param patientId El ID del paciente
     * @return Lista de sesiones del paciente
     */
    List<ChatSessionEntity> findByPatientId(Long patientId);

    /**
     * Encuentra todas las sesiones de chat de un practicante por su ID.
     *
     * @param practitionerId El ID del practicante
     * @return Lista de sesiones del practicante
     */
    List<ChatSessionEntity> findByPractitionerId(Long practitionerId);
}
