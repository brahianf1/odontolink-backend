package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Treatment;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para las ofertas de tratamientos del practicante (catálogo personal).
 * Puerto de salida (Output Port) en arquitectura hexagonal.
 */
public interface OfferedTreatmentRepository {

    /**
     * Guarda un nuevo tratamiento ofrecido o actualiza uno existente.
     */
    OfferedTreatment save(OfferedTreatment offeredTreatment);

    /**
     * Busca un tratamiento ofrecido por su ID.
     */
    Optional<OfferedTreatment> findById(Long id);

    /**
     * Obtiene todas las ofertas de tratamientos de un practicante específico.
     */
    List<OfferedTreatment> findByPractitioner(Practitioner practitioner);

    /**
     * Obtiene todas las ofertas de tratamientos de un practicante por su ID.
     */
    List<OfferedTreatment> findByPractitionerId(Long practitionerId);

    /**
     * Verifica si un practicante ya ofrece un tratamiento específico.
     * Esto evita duplicados en el catálogo personal.
     */
    boolean existsByPractitionerAndTreatment(Practitioner practitioner, Treatment treatment);

    /**
     * Busca un tratamiento ofrecido específico por practicante y tratamiento.
     */
    Optional<OfferedTreatment> findByPractitionerAndTreatment(Practitioner practitioner, Treatment treatment);

    /**
     * Elimina un tratamiento ofrecido por su ID.
     */
    void deleteById(Long id);

    /**
     * Verifica si existen turnos activos asociados a una oferta de tratamiento.
     */
    boolean hasActiveAppointments(Long offeredTreatmentId);

    /**
     * Obtiene todas las ofertas de tratamientos disponibles (catálogo público).
     * Usado para mostrar el catálogo completo a los pacientes.
     */
    List<OfferedTreatment> findAll();

    /**
     * Obtiene todas las ofertas de tratamientos filtradas por tipo de tratamiento.
     * Usado para mostrar ofertas específicas de un tratamiento particular.
     */
    List<OfferedTreatment> findByTreatmentId(Long treatmentId);
}
