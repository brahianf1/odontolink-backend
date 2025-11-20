package site.utnpf.odontolink.domain.service.slotstrategy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Estrategia que genera slots basándose en la duración específica del tratamiento.
 *
 * En este enfoque, los slots se generan secuencialmente según la duración del servicio.
 * Si un tratamiento dura 45 minutos, los slots serán: 08:00, 08:45, 09:30, etc.
 * Esto alinea los horarios ofrecidos con la configuración exacta del practicante.
 */
public class DynamicDurationSlotStrategy implements SlotGenerationStrategy {

    @Override
    public List<LocalDateTime> generateTheoreticalSlots(
            LocalDate date,
            LocalTime blockStart,
            LocalTime blockEnd,
            int serviceDuration) {

        List<LocalDateTime> slots = new ArrayList<>();
        LocalTime currentTime = blockStart;

        // Validación de seguridad para evitar bucles infinitos si la duración es 0 o negativa
        if (serviceDuration <= 0) {
            return slots;
        }

        while (currentTime.isBefore(blockEnd)) {
            // Verificar si el servicio completo cabe en el bloque
            LocalTime serviceEndTime = currentTime.plusMinutes(serviceDuration);

            if (serviceEndTime.isAfter(blockEnd)) {
                // El servicio se extendería más allá del bloque, no agregar este slot
                break;
            }

            // Agregar el slot como un LocalDateTime completo
            slots.add(LocalDateTime.of(date, currentTime));

            // Avanzar según la duración del tratamiento
            currentTime = currentTime.plusMinutes(serviceDuration);
        }

        return slots;
    }
}

