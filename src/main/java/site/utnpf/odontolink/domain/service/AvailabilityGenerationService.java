package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.domain.model.AppointmentStatus;
import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de Dominio que implementa el "Rulebook" para la generación de inventario dinámico de turnos.
 *
 * Este servicio es el corazón del nuevo modelo de agendamiento. Calcula los slots disponibles
 * en tiempo real basándose en:
 * 1. Los bloques de disponibilidad del practicante (AvailabilitySlot)
 * 2. La duración del servicio (durationInMinutes del OfferedTreatment)
 * 3. Los turnos ya reservados (Appointments en la "Agenda" del practicante)
 *
 * Responsabilidades principales:
 * - Generar slots discretos (08:00, 08:30, 09:00, etc.) a partir de bloques continuos
 * - Filtrar slots que colisionan con turnos ya reservados
 * - Devolver solo los slots realmente disponibles para reservar
 *
 * Este servicio opera exclusivamente con POJOs de dominio, sin conocimiento de infraestructura.
 *
 * @author OdontoLink Team
 */
public class AvailabilityGenerationService {

    private final AppointmentRepository appointmentRepository;
    private final OfferedTreatmentRepository offeredTreatmentRepository;

    /**
     * Intervalo en minutos para generar los slots (cada cuántos minutos se puede reservar).
     * Típicamente 30 minutos para la mayoría de servicios odontológicos.
     */
    private static final int SLOT_INTERVAL_MINUTES = 30;

    public AvailabilityGenerationService(AppointmentRepository appointmentRepository,
                                         OfferedTreatmentRepository offeredTreatmentRepository) {
        this.appointmentRepository = appointmentRepository;
        this.offeredTreatmentRepository = offeredTreatmentRepository;
    }

    /**
     * Genera los slots de tiempo disponibles para un tratamiento ofrecido en una fecha específica.
     *
     * Este es el método principal que implementa el algoritmo de inventario dinámico:
     * 1. Identifica el bloque de disponibilidad (AvailabilitySlot) para el día de la semana solicitado
     * 2. Genera una lista de slots teóricos basándose en la duración del servicio
     * 3. Consulta los turnos ya reservados del practicante para ese día
     * 4. Filtra los slots que colisionan con los turnos existentes
     * 5. Devuelve solo los slots disponibles
     *
     * Ejemplo:
     * - Bloque: Lunes 08:00-12:00
     * - Duración: 60 minutos
     * - Turnos reservados: 09:00-10:00
     * - Slots generados: [08:00, 08:30, 09:00, 09:30, 10:00, 10:30, 11:00]
     * - Slots filtrados (excluidos por colisión con 09:00-10:00): [09:00, 09:30]
     * - Resultado: [08:00, 08:30, 10:00, 10:30, 11:00]
     *
     * @param offeredTreatment La oferta de tratamiento (contiene duración y bloques de disponibilidad)
     * @param requestedDate La fecha para la cual se solicitan los horarios
     * @return Lista de LocalDateTime con los slots disponibles (timestamps exactos como 2025-01-15T08:00)
     */
    public List<LocalDateTime> generateAvailableSlots(OfferedTreatment offeredTreatment, LocalDate requestedDate) {

        // 1. Obtener el día de la semana de la fecha solicitada
        DayOfWeek dayOfWeek = requestedDate.getDayOfWeek();

        // 2. Buscar el AvailabilitySlot que corresponde a ese día de la semana
        AvailabilitySlot matchingSlot = findAvailabilitySlotForDay(offeredTreatment, dayOfWeek);

        // Si no hay disponibilidad para ese día, devolver lista vacía
        if (matchingSlot == null) {
            return new ArrayList<>();
        }

        // 3. Obtener la duración del servicio
        int durationInMinutes = offeredTreatment.getDurationInMinutes();

        // 4. Generar los slots teóricos para ese bloque de disponibilidad
        List<LocalDateTime> theoreticalSlots = generateTheoreticalSlots(
            requestedDate,
            matchingSlot.getStartTime(),
            matchingSlot.getEndTime(),
            durationInMinutes
        );

        // 5. Obtener los turnos ya reservados del practicante para ese día
        List<Appointment> existingAppointments = getActiveAppointmentsForDate(
            offeredTreatment.getPractitioner().getId(),
            requestedDate
        );

        // 6. Filtrar los slots que colisionan con los turnos existentes
        return filterAvailableSlots(theoreticalSlots, existingAppointments, durationInMinutes);
    }

    /**
     * Busca el AvailabilitySlot que corresponde al día de la semana especificado.
     *
     * @param offeredTreatment La oferta de tratamiento
     * @param dayOfWeek El día de la semana buscado
     * @return El AvailabilitySlot correspondiente, o null si no hay disponibilidad para ese día
     */
    private AvailabilitySlot findAvailabilitySlotForDay(OfferedTreatment offeredTreatment, DayOfWeek dayOfWeek) {
        if (offeredTreatment.getAvailabilitySlots() == null) {
            return null;
        }

        return offeredTreatment.getAvailabilitySlots().stream()
            .filter(slot -> slot.getDayOfWeek() == dayOfWeek)
            .findFirst()
            .orElse(null);
    }

    /**
     * Genera una lista de slots teóricos para un bloque de disponibilidad.
     *
     * Los slots se generan cada SLOT_INTERVAL_MINUTES (típicamente 30 minutos).
     * Solo se incluyen slots donde el servicio completo puede completarse dentro del bloque.
     *
     * Ejemplo:
     * - Bloque: 08:00-12:00 (4 horas)
     * - Duración servicio: 60 minutos
     * - Intervalo: 30 minutos
     * - Slots generados: 08:00, 08:30, 09:00, 09:30, 10:00, 10:30, 11:00
     *   (11:30 se excluye porque el servicio terminaría a las 12:30, fuera del bloque)
     *
     * @param date La fecha para los slots
     * @param blockStart Hora de inicio del bloque de disponibilidad
     * @param blockEnd Hora de fin del bloque de disponibilidad
     * @param serviceDuration Duración del servicio en minutos
     * @return Lista de timestamps representando cada slot teórico
     */
    private List<LocalDateTime> generateTheoreticalSlots(
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

            // Avanzar al siguiente intervalo
            currentTime = currentTime.plusMinutes(SLOT_INTERVAL_MINUTES);
        }

        return slots;
    }

    /**
     * Obtiene los turnos activos del practicante para una fecha específica.
     *
     * Se excluyen los turnos cancelados ya que no representan un conflicto real.
     *
     * @param practitionerId ID del practicante
     * @param date Fecha para la cual buscar turnos
     * @return Lista de Appointments activos para esa fecha
     */
    private List<Appointment> getActiveAppointmentsForDate(Long practitionerId, LocalDate date) {
        // Obtener todos los turnos del practicante para ese día que no estén cancelados
        List<Appointment> allAppointments = appointmentRepository.findByPractitionerId(practitionerId);

        return allAppointments.stream()
            .filter(apt -> apt.getStatus() != AppointmentStatus.CANCELLED)
            .filter(apt -> apt.getAppointmentTime().toLocalDate().equals(date))
            .collect(Collectors.toList());
    }

    /**
     * Filtra los slots teóricos, eliminando aquellos que colisionan con turnos existentes.
     *
     * Un slot colisiona con un turno existente si el rango de tiempo del servicio
     * se solapa con el rango de tiempo del turno reservado.
     *
     * Algoritmo de detección de colisión:
     * - Slot propuesto: [slotStart, slotStart + serviceDuration]
     * - Turno existente: [appointmentStart, appointmentEnd]
     * - Hay colisión si los rangos se solapan
     *
     * @param theoreticalSlots Lista de slots teóricos generados
     * @param existingAppointments Turnos ya reservados del practicante
     * @param serviceDuration Duración del servicio en minutos
     * @return Lista filtrada conteniendo solo los slots disponibles (sin colisiones)
     */
    private List<LocalDateTime> filterAvailableSlots(
            List<LocalDateTime> theoreticalSlots,
            List<Appointment> existingAppointments,
            int serviceDuration) {

        List<LocalDateTime> availableSlots = new ArrayList<>();

        for (LocalDateTime slotStart : theoreticalSlots) {
            LocalDateTime slotEnd = slotStart.plusMinutes(serviceDuration);

            // Verificar si este slot colisiona con algún turno existente
            boolean hasCollision = existingAppointments.stream()
                .anyMatch(appointment -> {
                    LocalDateTime appointmentStart = appointment.getAppointmentTime();
                    int appointmentDuration = appointment.getDurationInMinutes();
                    LocalDateTime appointmentEnd = appointmentStart.plusMinutes(appointmentDuration);

                    // Detectar solapamiento: dos rangos se solapan si uno comienza antes de que el otro termine
                    return slotStart.isBefore(appointmentEnd) && slotEnd.isAfter(appointmentStart);
                });

            // Si no hay colisión, el slot está disponible
            if (!hasCollision) {
                availableSlots.add(slotStart);
            }
        }

        return availableSlots;
    }

}
