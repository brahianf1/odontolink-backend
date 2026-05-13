package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.OfferedTreatmentSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
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
     * Verifica si un practicante ya ofrece ACTIVAMENTE un tratamiento específico.
     *
     * Sólo cuentan las ofertas {@code active=true}: si la oferta fue dada de
     * baja lógica por RF16, no debe bloquear la creación de una nueva. La
     * fila desactivada queda como registro histórico pero no compite por
     * la unicidad del catálogo personal vigente.
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
     * Verifica si existen turnos SCHEDULED a futuro asociados a la oferta.
     *
     * La asociación se establece por el par practitioner+treatment a través
     * de la cadena Appointment → Attention. Es la regla que dispara la Baja
     * Lógica de RF16 cuando se intenta eliminar la oferta.
     *
     * @param offeredTreatmentId ID de la oferta a evaluar
     * @return true si existe al menos un turno SCHEDULED con
     *         appointmentTime ≥ now() asociado a la oferta
     */
    boolean hasFutureScheduledAppointments(Long offeredTreatmentId);

    /**
     * Verifica si existen Atenciones IN_PROGRESS para el par
     * practitioner+treatment al que pertenece esta oferta.
     *
     * Una atención activa significa que hay un caso clínico abierto que
     * todavía puede recibir nuevos turnos o evolutivos: tampoco es seguro
     * borrar físicamente la oferta.
     *
     * @param offeredTreatmentId ID de la oferta a evaluar
     * @return true si existe al menos una Attention IN_PROGRESS
     */
    boolean hasInProgressAttentions(Long offeredTreatmentId);

    /**
     * Verifica si existen Atenciones de cualquier estado (históricas)
     * para el par practitioner+treatment de la oferta.
     *
     * Aunque no haya compromisos vivos, la presencia de atenciones
     * históricas (COMPLETED/CANCELLED) sugiere preservar la oferta como
     * registro de catálogo histórico: se prefiere Baja Lógica también
     * para no perder la cadena de navegación clínica.
     *
     * @param offeredTreatmentId ID de la oferta a evaluar
     * @return true si existe al menos una Attention asociada al par
     */
    boolean hasHistoricalAttentions(Long offeredTreatmentId);

    /**
     * Obtiene todas las ofertas de tratamientos disponibles (catálogo público).
     * Usado para mostrar el catálogo completo a los pacientes.
     *
     * Sólo retorna ofertas con {@code active=true}: las baja-lógica son
     * invisibles al catálogo público (RF16).
     */
    List<OfferedTreatment> findAll();

    /**
     * Obtiene todas las ofertas de tratamientos filtradas por tipo de tratamiento.
     * Sólo retorna ofertas con {@code active=true}.
     */
    List<OfferedTreatment> findByTreatmentId(Long treatmentId);

    /**
     * Búsqueda paginada y filtrada del catálogo público (RF09).
     *
     * Aplica los criterios opcionales en AND lógico y devuelve una
     * {@link PageResult} con la slice solicitada y la metadata necesaria
     * para que el cliente arme la paginación visual.
     *
     * Sólo retorna ofertas con {@code active=true}; nunca expone bajas
     * lógicas al paciente.
     *
     * @param criteria Criterios opcionales (keyword/specialty/availability)
     * @param pageQuery Página solicitada y ordenamiento
     * @return Página de resultados (puede ser vacía, nunca null)
     */
    PageResult<OfferedTreatment> search(OfferedTreatmentSearchCriteria criteria, PageQuery pageQuery);
}
