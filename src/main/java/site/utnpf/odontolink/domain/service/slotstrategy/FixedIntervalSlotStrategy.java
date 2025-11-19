package site.utnpf.odontolink.domain.service.slotstrategy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Estrategia que genera slots con un intervalo fijo predefinido (30 minutos),
 * independientemente de la duración del tratamiento.
 *
 * Este enfoque fomenta la competencia por los slots, permitiendo que tratamientos
 * de diferente duración se ofrezcan en los mismos horarios base (ej: todos comienzan a las XX:00 o XX:30).
 */
public class FixedIntervalSlotStrategy implements SlotGenerationStrategy {

    private static final int SLOT_INTERVAL_MINUTES = 30;

    @Override
    public List<LocalDateTime> generateTheoreticalSlots(
            LocalDate date,
            LocalTime blockStart,
            LocalTime blockEnd,
            int serviceDuration) {

        List<LocalDateTime> slots = new ArrayList<>();
        LocalTime currentTime = blockStart;

        while (currentTime.isBefore(blockEnd)) {
            // Verificar si el servicio completo cabe en el bloque
            LocalTime serviceEndTime = currentTime.plusMinutes(serviceDuration);

            if (serviceEndTime.isAfter(blockEnd)) {
                // El servicio se extendería más allá del bloque, no agregar este slot
                break;
            }

            // Agregar el slot como un LocalDateTime completo
            slots.add(LocalDateTime.of(date, currentTime));

            // Avanzar al siguiente intervalo fijo
            currentTime = currentTime.plusMinutes(SLOT_INTERVAL_MINUTES);
        }

        return slots;
    }
}

