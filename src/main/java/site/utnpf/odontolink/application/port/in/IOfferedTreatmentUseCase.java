package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.OfferedTreatmentDeletionResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Puerto de entrada (Input Port) para casos de uso relacionados con el catálogo personal de tratamientos.
 * Define las operaciones que un practicante puede realizar con su catálogo de tratamientos ofrecidos.
 *
 * Este puerto trabaja exclusivamente con objetos de dominio, manteniendo la capa de aplicación
 * independiente de la infraestructura. La conversión de DTOs a objetos de dominio se realiza
 * en los adaptadores de entrada (controladores con ayuda de mappers).
 *
 * @author OdontoLink Team
 */
public interface IOfferedTreatmentUseCase {

    /**
     * Agrega un tratamiento al catálogo personal del practicante.
     * Corresponde al CU-005 (Agregar Tratamiento al Catálogo Personal).
     *
     * @param practitionerId ID del practicante (obtenido del usuario autenticado)
     * @param treatmentId ID del tratamiento maestro seleccionado
     * @param requirements Requisitos específicos del practicante
     * @param durationInMinutes Duración del tratamiento en minutos
     * @param availabilitySlots Horarios de disponibilidad (objetos de dominio)
     * @param offerStartDate Fecha de inicio de la oferta
     * @param offerEndDate Fecha de fin de la oferta
     * @param maxCompletedAttentions Cupo máximo de casos completados
     * @return El tratamiento ofrecido creado
     */
    OfferedTreatment addTreatmentToCatalog(Long practitionerId,
                                           Long treatmentId,
                                           String requirements,
                                           int durationInMinutes,
                                           Set<AvailabilitySlot> availabilitySlots,
                                           LocalDate offerStartDate,
                                           LocalDate offerEndDate,
                                           Integer maxCompletedAttentions);

    /**
     * Modifica un tratamiento del catálogo personal del practicante.
     * Corresponde al CU-006 (Modificar Tratamiento del Catálogo Personal).
     *
     * @param practitionerId ID del practicante (para verificar permisos)
     * @param offeredTreatmentId ID del tratamiento ofrecido a modificar
     * @param requirements Nuevos requisitos específicos
     * @param durationInMinutes Duración del tratamiento en minutos (null para no modificar)
     * @param availabilitySlots Nuevos horarios de disponibilidad (objetos de dominio)
     * @param offerStartDate Nueva fecha de inicio (null para no modificar)
     * @param offerEndDate Nueva fecha de fin (null para no modificar)
     * @param maxCompletedAttentions Nuevo cupo máximo (null para no modificar)
     * @return El tratamiento ofrecido actualizado
     */
    OfferedTreatment updateOfferedTreatment(Long practitionerId,
                                            Long offeredTreatmentId,
                                            String requirements,
                                            Integer durationInMinutes,
                                            Set<AvailabilitySlot> availabilitySlots,
                                            LocalDate offerStartDate,
                                            LocalDate offerEndDate,
                                            Integer maxCompletedAttentions);

    /**
     * Reactiva una oferta dada de baja lógicamente (INACTIVE → ACTIVE).
     * El practicante autenticado debe ser dueño y la oferta debe estar en
     * estado INACTIVE; cualquier otro estado responde 409.
     *
     * @param practitionerId ID del practicante autenticado
     * @param offeredTreatmentId ID de la oferta a reactivar
     * @return La oferta reactivada
     */
    OfferedTreatment reactivateOfferedTreatment(Long practitionerId, Long offeredTreatmentId);

    /**
     * Pausa voluntariamente una oferta vigente (ACTIVE → PAUSED).
     * La oferta deja de aparecer en el catálogo público y deja de aceptar
     * nuevas reservas, pero los turnos y atenciones ya existentes siguen
     * su curso. La oferta debe estar ACTIVE; cualquier otro estado responde 409.
     *
     * @param practitionerId ID del practicante autenticado
     * @param offeredTreatmentId ID de la oferta a pausar
     * @return La oferta en estado PAUSED
     */
    OfferedTreatment pauseOfferedTreatment(Long practitionerId, Long offeredTreatmentId);

    /**
     * Reanuda una oferta pausada (PAUSED → ACTIVE).
     *
     * @param practitionerId ID del practicante autenticado
     * @param offeredTreatmentId ID de la oferta a reanudar
     * @return La oferta en estado ACTIVE
     */
    OfferedTreatment resumeOfferedTreatment(Long practitionerId, Long offeredTreatmentId);

    /**
     * Elimina (o desactiva por integridad) un tratamiento del catálogo personal
     * del practicante. Implementa la política de RF16:
     *
     * - Si la oferta tiene compromisos vivos (turnos SCHEDULED futuros o
     *   Atenciones IN_PROGRESS) o atenciones históricas, se aplica BAJA LÓGICA
     *   (active = false) para preservar la cadena referencial de turnos y
     *   atenciones ya otorgados.
     * - Si la oferta nunca fue usada, se elimina físicamente.
     *
     * Verificación de propiedad: el practicante autenticado DEBE ser el dueño
     * de la oferta. La identidad se obtiene del JWT y se enfrenta contra
     * {@code offer.practitioner.id}. Cualquier discrepancia dispara
     * {@code UnauthorizedOperationException}.
     *
     * Corresponde al CU-007 (Eliminar Tratamiento del Catálogo Personal).
     *
     * @param practitionerId ID del practicante autenticado (del JWT)
     * @param offeredTreatmentId ID del tratamiento ofrecido a eliminar
     * @return El outcome del Dominio (SOFT/HARD) con la razón asociada
     */
    OfferedTreatmentDeletionResult removeFromCatalog(Long practitionerId, Long offeredTreatmentId);

    /**
     * Obtiene los tratamientos del catálogo personal del practicante,
     * filtrados según el bucket solicitado. Usado para "Mi Catálogo Personal".
     *
     * El filtrado se aplica en memoria sobre el resultado del repositorio:
     * el catálogo personal de un practicante no escala a volúmenes que
     * justifiquen empujar el predicado a la BD (esperable < 100 filas).
     *
     * @param practitionerId ID del practicante
     * @param filter Bucket a devolver. Ver {@link OfferedTreatmentListFilter}.
     * @return Lista de ofertas que satisfacen el filtro, posiblemente vacía
     */
    List<OfferedTreatment> getMyOfferedTreatments(Long practitionerId, OfferedTreatmentListFilter filter);

    /**
     * Busca un tratamiento ofrecido por su ID.
     *
     * @param offeredTreatmentId ID del tratamiento ofrecido
     * @return El tratamiento ofrecido encontrado
     */
    OfferedTreatment getOfferedTreatmentById(Long offeredTreatmentId);

    /**
     * Obtiene el progreso (cantidad de atenciones completadas) para todas las ofertas
     * de un practicante, agrupadas por tratamiento.
     *
     * @param practitionerId ID del practicante
     * @return Map donde la clave es el ID del tratamiento y el valor es la cantidad de atenciones completadas
     */
    Map<Long, Long> getCompletedAttentionsProgressForPractitioner(Long practitionerId);

    /**
     * Obtiene la carga de trabajo actual (cantidad de atenciones activas/en progreso)
     * para todas las ofertas de un practicante, agrupadas por tratamiento.
     *
     * @param practitionerId ID del practicante
     * @return Map donde la clave es el ID del tratamiento y el valor es la cantidad de atenciones activas
     */
    Map<Long, Long> getActiveAttentionsProgressForPractitioner(Long practitionerId);

    /**
     * Obtiene la cantidad de atenciones canceladas históricamente
     * para todas las ofertas de un practicante, agrupadas por tratamiento.
     *
     * @param practitionerId ID del practicante
     * @return Map donde la clave es el ID del tratamiento y el valor es la cantidad de atenciones canceladas
     */
    Map<Long, Long> getCancelledAttentionsProgressForPractitioner(Long practitionerId);
}
