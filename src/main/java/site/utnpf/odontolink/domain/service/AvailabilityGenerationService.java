package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.model.*;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de Dominio que implementa el "Rulebook" para la generación de inventario dinámico de turnos.
 *
 * Este servicio es el corazón del modelo de agendamiento con ofertas finitas. Calcula los slots disponibles
 * en tiempo real basándose en:
 * 1. Límites temporales de la oferta (offerStartDate, offerEndDate)
 * 2. Límites de cupo de la oferta (maxCompletedAttentions)
 * 3. Los bloques de disponibilidad del practicante (AvailabilitySlot)
 * 4. La duración del servicio (durationInMinutes del OfferedTreatment)
 * 5. Los turnos ya reservados (Appointments en la "Agenda" del practicante)
 *
 * Filosofía "Lo que suceda primero":
 * La oferta deja de estar disponible cuando se cumple UNA de las siguientes condiciones:
 * - La fecha solicitada está fuera del rango (offerStartDate - offerEndDate)
 * - Se alcanzó el cupo máximo de casos completados (maxCompletedAttentions)
 *
 * Responsabilidades principales:
 * - Validar límites temporales de la oferta
 * - Validar límites de cupo (stock) de la oferta
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
    private final AttentionRepository attentionRepository;

    /**
     * Intervalo en minutos para generar los slots (cada cuántos minutos se puede reservar).
     * Típicamente 30 minutos para la mayoría de servicios odontológicos.
     */
    private static final int SLOT_INTERVAL_MINUTES = 30;

    public AvailabilityGenerationService(AppointmentRepository appointmentRepository,
                                         OfferedTreatmentRepository offeredTreatmentRepository,
                                         AttentionRepository attentionRepository) {
        this.appointmentRepository = appointmentRepository;
        this.offeredTreatmentRepository = offeredTreatmentRepository;
        this.attentionRepository = attentionRepository;
    }

    /**
     * Genera los slots de tiempo disponibles para un tratamiento ofrecido en una fecha específica.
     *
     * Este método implementa el "Rulebook" con validaciones de ofertas finitas:
     *
     * VALIDACIÓN 1: Límite de Tiempo
     * - Verifica que la fecha solicitada esté dentro del rango (offerStartDate - offerEndDate)
     * - Si está fuera del rango, devuelve lista vacía
     *
     * VALIDACIÓN 2: Límite de Cupo (Meta Académica)
     * - Cuenta (COMPLETED + IN_PROGRESS) del practicante para este tratamiento.
     * - Si la suma alcanza el cupo máximo, devuelve lista vacía.
     * - Esto cubre tanto la protección contra sobrecarga (muchos activos)
     *   como el cumplimiento de la meta académica (ya completó los requeridos).
     *
     * VALIDACIÓN 3: Inventario Dinámico Diario
     * - Solo si pasó las validaciones 1 y 2, calcula el inventario:
     *   1. Identifica el bloque de disponibilidad para el día solicitado
     *   2. Genera slots teóricos basándose en la duración del servicio
     *   3. Consulta los turnos ya reservados del practicante
     *   4. Filtra los slots que colisionan con los turnos existentes
     *   5. Filtra los slots que ya pasaron (solo si es el día actual)
     *
     * Ejemplo:
     * - Oferta: 2025-01-01 a 2025-06-30, cupo 10 casos
     * - Fecha solicitada: 2025-03-15
     * - Casos: 6 Completados + 4 En Progreso = 10 (Cupo lleno)
     * - Resultado: [] (Lista vacía, no se ofrecen turnos)
     *
     * @param offeredTreatment La oferta de tratamiento (contiene límites, duración y bloques)
     * @param requestedDate La fecha para la cual se solicitan los horarios
     * @return Lista de LocalDateTime con los slots disponibles, o lista vacía si la oferta no es válida
     */
    public List<LocalDateTime> generateAvailableSlots(OfferedTreatment offeredTreatment, LocalDate requestedDate) {

        // VALIDACIÓN 1: Límite de Tiempo
        if (!isWithinOfferDateRange(offeredTreatment, requestedDate)) {
            return new ArrayList<>();
        }

        // VALIDACIÓN 2: Límite de Cupo (Stock)
        if (hasReachedMaxCapacity(offeredTreatment)) {
            return new ArrayList<>();
        }

        // VALIDACIÓN 3: Inventario Dinámico Diario

        DayOfWeek dayOfWeek = requestedDate.getDayOfWeek();
        AvailabilitySlot matchingSlot = findAvailabilitySlotForDay(offeredTreatment, dayOfWeek);

        if (matchingSlot == null) {
            return new ArrayList<>();
        }

        int durationInMinutes = offeredTreatment.getDurationInMinutes();

        List<LocalDateTime> theoreticalSlots = generateTheoreticalSlots(
            requestedDate,
            matchingSlot.getStartTime(),
            matchingSlot.getEndTime(),
            durationInMinutes
        );

        List<Appointment> existingAppointments = getActiveAppointmentsForDate(
            offeredTreatment.getPractitioner().getId(),
            requestedDate
        );

        List<LocalDateTime> availableSlots = filterAvailableSlots(theoreticalSlots, existingAppointments, durationInMinutes);

        return filterPastSlots(availableSlots, requestedDate);
    }

    /**
     * Valida que la fecha solicitada esté dentro del rango de la oferta.
     *
     * Los campos offerStartDate y offerEndDate son obligatorios,
     * por lo que siempre existirán valores para validar.
     *
     * @param offeredTreatment La oferta de tratamiento
     * @param requestedDate La fecha solicitada
     * @return true si la fecha está dentro del rango, false en caso contrario
     */
    private boolean isWithinOfferDateRange(OfferedTreatment offeredTreatment, LocalDate requestedDate) {
        LocalDate startDate = offeredTreatment.getOfferStartDate();
        LocalDate endDate = offeredTreatment.getOfferEndDate();

        if (requestedDate.isBefore(startDate)) {
            return false;
        }

        if (requestedDate.isAfter(endDate)) {
            return false;
        }

        return true;
    }

    /**
     * Verifica si se alcanzó el cupo máximo (Meta Académica).
     *
     * REGLA DE NEGOCIO (Refinada):
     * El cupo representa la "Meta Académica" del practicante.
     * Se considera alcanzado cuando la suma de casos COMPLETED (meta cumplida)
     * más los casos IN_PROGRESS (compromisos activos) iguala o supera el límite.
     *
     * Escenarios:
     * 1. Protección de Carga: Si tengo 5 completas y 5 en curso (Total 10/10),
     *    se cierra la oferta. Si cancelo una en curso, baja a 9/10 y se reabre.
     * 2. Meta Cumplida: Si tengo 10 completas (Total 10/10), se cierra la oferta
     *    porque ya cumplí. Si necesito atender más, debo aumentar mi cupo manualmente.
     *
     * @param offeredTreatment La oferta de tratamiento
     * @return true si se alcanzó el cupo máximo, false en caso contrario
     */
    private boolean hasReachedMaxCapacity(OfferedTreatment offeredTreatment) {
        // Si maxCompletedAttentions es null, asumimos que no hay límite
        if (offeredTreatment.getMaxCompletedAttentions() == null) {
            return false;
        }

        int maxCupo = offeredTreatment.getMaxCompletedAttentions();

        // Contar IN_PROGRESS (Compromisos activos)
        int activeCount = attentionRepository.countByPractitionerAndTreatmentAndStatus(
            offeredTreatment.getPractitioner(),
            offeredTreatment.getTreatment(),
            AttentionStatus.IN_PROGRESS
        );

        // Contar COMPLETED (Meta ya cumplida)
        int completedCount = attentionRepository.countByPractitionerAndTreatmentAndStatus(
            offeredTreatment.getPractitioner(),
            offeredTreatment.getTreatment(),
            AttentionStatus.COMPLETED
        );

        int totalConsumedQuota = activeCount + completedCount;

        return totalConsumedQuota >= maxCupo;
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

    /**
     * Filtra los slots que ya pasaron cuando la fecha solicitada es el día actual.
     * Esto evita que se muestren horarios que ya transcurrieron.
     *
     * @param slots Lista de slots disponibles
     * @param requestedDate Fecha para la cual se generaron los slots
     * @return Lista filtrada sin los slots pasados (si es hoy) o la lista completa (si es fecha futura)
     */
    private List<LocalDateTime> filterPastSlots(List<LocalDateTime> slots, LocalDate requestedDate) {
        LocalDate today = LocalDate.now();

        // Si la fecha solicitada no es hoy, devolver todos los slots
        if (!requestedDate.equals(today)) {
            return slots;
        }

        // Si es hoy, filtrar los slots que ya pasaron
        LocalDateTime now = LocalDateTime.now();
        return slots.stream()
                .filter(slot -> slot.isAfter(now))
                .collect(Collectors.toList());
    }

}
