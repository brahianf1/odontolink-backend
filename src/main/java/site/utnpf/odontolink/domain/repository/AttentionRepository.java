package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.AttentionStatus;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Treatment;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para Attention (Atención/Caso Clínico).
 * Puerto de salida (Output Port) en arquitectura hexagonal.
 */
public interface AttentionRepository {

    /**
     * Guarda una nueva atención o actualiza una existente.
     * Este método persiste también los Appointments asociados gracias a CascadeType.ALL.
     */
    Attention save(Attention attention);

    /**
     * Busca una atención por su ID.
     */
    Optional<Attention> findById(Long id);

    /**
     * Obtiene todas las atenciones de un paciente específico.
     */
    List<Attention> findByPatient(Patient patient);

    /**
     * Obtiene todas las atenciones de un paciente por su ID.
     */
    List<Attention> findByPatientId(Long patientId);

    /**
     * Obtiene todas las atenciones de un practicante específico.
     */
    List<Attention> findByPractitioner(Practitioner practitioner);

    /**
     * Obtiene todas las atenciones de un practicante por su ID.
     */
    List<Attention> findByPractitionerId(Long practitionerId);

    /**
     * Obtiene las atenciones de un paciente filtradas por estado.
     */
    List<Attention> findByPatientIdAndStatus(Long patientId, AttentionStatus status);

    /**
     * Obtiene las atenciones de un practicante filtradas por estado.
     */
    List<Attention> findByPractitionerIdAndStatus(Long practitionerId, AttentionStatus status);

    /**
     * Verifica si existe una atención activa (EN_PROGRESO) entre un paciente y practicante
     * para un tratamiento específico.
     * Útil para evitar duplicar casos clínicos abiertos.
     */
    boolean existsByPatientIdAndPractitionerIdAndTreatmentIdAndStatus(
            Long patientId,
            Long practitionerId,
            Long treatmentId,
            AttentionStatus status
    );

    /**
     * Busca una atención activa entre un paciente, practicante y tratamiento específico.
     */
    Optional<Attention> findByPatientIdAndPractitionerIdAndTreatmentIdAndStatus(
            Long patientId,
            Long practitionerId,
            Long treatmentId,
            AttentionStatus status
    );

    /**
     * Cuenta el número de atenciones completadas para un practicante y tratamiento específicos.
     * Este método es fundamental para el sistema de ofertas finitas:
     * permite verificar si se alcanzó el cupo máximo de casos completados.
     *
     * @param practitioner El practicante
     * @param treatment El tratamiento
     * @param status El estado de las atenciones a contar (típicamente COMPLETED)
     * @return Cantidad de atenciones en el estado especificado
     */
    int countByPractitionerAndTreatmentAndStatus(
            Practitioner practitioner,
            Treatment treatment,
            AttentionStatus status
    );
}
