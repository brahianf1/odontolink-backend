package site.utnpf.odontolink.domain.service.slotstrategy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Estrategia para la generación de slots de tiempo teóricos.
 * Define cómo se dividen los bloques de disponibilidad en slots individuales.
 */
public interface SlotGenerationStrategy {

    /**
     * Genera una lista de slots teóricos para un bloque de disponibilidad dado.
     *
     * @param date La fecha para los slots
     * @param blockStart Hora de inicio del bloque de disponibilidad
     * @param blockEnd Hora de fin del bloque de disponibilidad
     * @param serviceDuration Duración del servicio en minutos
     * @return Lista de timestamps representando cada slot teórico
     */
    List<LocalDateTime> generateTheoreticalSlots(
            LocalDate date,
            LocalTime blockStart,
            LocalTime blockEnd,
            int serviceDuration);
}

