package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.AvailabilitySlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para AvailabilitySlot (Franja de Disponibilidad).
 * Puerto de salida (Output Port) en arquitectura hexagonal.
 */
public interface AvailabilitySlotRepository {

    /**
     * Guarda una nueva franja de disponibilidad o actualiza una existente.
     */
    AvailabilitySlot save(AvailabilitySlot availabilitySlot);

    /**
     * Busca una franja de disponibilidad por su ID.
     */
    Optional<AvailabilitySlot> findById(Long id);

    /**
     * Obtiene todas las franjas de disponibilidad de un tratamiento ofrecido específico.
     */
    List<AvailabilitySlot> findByOfferedTreatmentId(Long offeredTreatmentId);

    /**
     * Verifica si un horario específico cae dentro de alguna franja de disponibilidad
     * de un tratamiento ofrecido.
     *
     * @param offeredTreatmentId ID del tratamiento ofrecido
     * @param dayOfWeek Día de la semana del turno solicitado
     * @param time Hora del turno solicitado
     * @return true si el horario está dentro de alguna franja válida
     */
    boolean isTimeWithinAvailability(
            Long offeredTreatmentId,
            DayOfWeek dayOfWeek,
            LocalTime time
    );
}
