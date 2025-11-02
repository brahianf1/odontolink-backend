package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.model.OfferedTreatment;

import java.util.List;
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
     * @return El tratamiento ofrecido creado
     */
    OfferedTreatment addTreatmentToCatalog(Long practitionerId,
                                           Long treatmentId,
                                           String requirements,
                                           int durationInMinutes,
                                           Set<AvailabilitySlot> availabilitySlots);

    /**
     * Modifica un tratamiento del catálogo personal del practicante.
     * Corresponde al CU-006 (Modificar Tratamiento del Catálogo Personal).
     *
     * @param practitionerId ID del practicante (para verificar permisos)
     * @param offeredTreatmentId ID del tratamiento ofrecido a modificar
     * @param requirements Nuevos requisitos específicos
     * @param durationInMinutes Duración del tratamiento en minutos (null para no modificar)
     * @param availabilitySlots Nuevos horarios de disponibilidad (objetos de dominio)
     * @return El tratamiento ofrecido actualizado
     */
    OfferedTreatment updateOfferedTreatment(Long practitionerId,
                                            Long offeredTreatmentId,
                                            String requirements,
                                            Integer durationInMinutes,
                                            Set<AvailabilitySlot> availabilitySlots);

    /**
     * Elimina un tratamiento del catálogo personal del practicante.
     * Corresponde al CU-007 (Eliminar Tratamiento del Catálogo Personal).
     *
     * @param practitionerId ID del practicante (para verificar permisos)
     * @param offeredTreatmentId ID del tratamiento ofrecido a eliminar
     */
    void removeFromCatalog(Long practitionerId, Long offeredTreatmentId);

    /**
     * Obtiene todos los tratamientos que ofrece un practicante específico.
     * Usado para mostrar "Mi Catálogo Personal" al practicante.
     *
     * @param practitionerId ID del practicante
     * @return Lista de tratamientos ofrecidos por el practicante
     */
    List<OfferedTreatment> getMyOfferedTreatments(Long practitionerId);

    /**
     * Busca un tratamiento ofrecido por su ID.
     *
     * @param offeredTreatmentId ID del tratamiento ofrecido
     * @return El tratamiento ofrecido encontrado
     */
    OfferedTreatment getOfferedTreatmentById(Long offeredTreatmentId);
}
