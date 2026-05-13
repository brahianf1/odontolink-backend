package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Puerto de entrada (Input Port) para casos de uso relacionados con turnos (appointments).
 *
 * Define las operaciones del modelo "intent-driven" de reserva, gestión de
 * asistencia y cancelación de turnos:
 * <ul>
 *   <li>Reserva instantánea con creación / agrupación automática de Atención.</li>
 *   <li>Gestión de asistencia: completar o marcar como no-asistió.</li>
 *   <li>Cancelaciones diferenciadas por actor (paciente vs. practicante).</li>
 *   <li>Lectura de turnos vigentes y catálogo público.</li>
 * </ul>
 *
 * Este puerto trabaja exclusivamente con objetos de dominio, manteniendo la capa de aplicación
 * independiente de la infraestructura. La conversión de DTOs a objetos de dominio se realiza
 * en los adaptadores de entrada (controladores con ayuda de mappers).
 *
 * @author OdontoLink Team
 */
public interface IAppointmentUseCase {

    /**
     * Reserva un turno aplicando el modelo "intent-driven" completo:
     * <ul>
     *   <li>Si no existe Atención IN_PROGRESS para el trío
     *       paciente/practicante/tratamiento, se crea atómicamente.</li>
     *   <li>Si existe, el nuevo turno se agrupa dentro de esa Atención.</li>
     *   <li>Se aplica la regla anti-acaparamiento dinámica leyendo el
     *       límite desde InstitutionalSettings (no hardcodeado).</li>
     * </ul>
     *
     * Corresponde al CU-008: Reservar Turno.
     *
     * @param patientId          ID del paciente autenticado que solicita el turno
     * @param offeredTreatmentId ID de la oferta de tratamiento seleccionada del catálogo
     * @param appointmentTime    Fecha y hora exactas del turno solicitado
     * @return La Attention creada o actualizada con el nuevo Appointment
     */
    Attention bookAppointment(Long patientId, Long offeredTreatmentId, LocalDateTime appointmentTime);

    /**
     * Obtiene todos los turnos agendados de un paciente específico.
     * Corresponde a la funcionalidad "Mis Turnos" del paciente.
     *
     * @param patientId ID del paciente
     * @return Lista de turnos del paciente
     */
    List<Appointment> getUpcomingAppointmentsForPatient(Long patientId);

    /**
     * Obtiene todos los turnos agendados de un practicante específico.
     * Corresponde a la funcionalidad "Mis Turnos" del practicante.
     *
     * @param practitionerId ID del practicante
     * @return Lista de turnos del practicante
     */
    List<Appointment> getUpcomingAppointmentsForPractitioner(Long practitionerId);

    /**
     * Obtiene todos los tratamientos ofrecidos disponibles (catálogo público).
     * Permite al paciente ver el catálogo de tratamientos.
     *
     * Este método puede filtrar opcionalmente por tratamiento específico.
     *
     * @param treatmentId ID del tratamiento para filtrar (opcional, puede ser null)
     * @return Lista de tratamientos ofrecidos disponibles
     */
    List<OfferedTreatment> getAvailableOfferedTreatments(Long treatmentId);

    /**
     * Obtiene los slots de tiempo disponibles para un tratamiento ofrecido en una fecha específica.
     * Este método implementa el cálculo de inventario dinámico.
     *
     * El servicio calcula en tiempo real:
     * 1. Los slots teóricos basados en el bloque de disponibilidad y la duración del servicio
     * 2. Filtra los slots que colisionan con turnos ya reservados
     * 3. Devuelve solo los slots realmente disponibles para reservar
     *
     * @param offeredTreatmentId ID de la oferta de tratamiento
     * @param requestedDate Fecha para la cual se consultan los horarios disponibles
     * @return Lista de LocalDateTime con los slots disponibles
     */
    List<LocalDateTime> getAvailableSlots(Long offeredTreatmentId, LocalDate requestedDate);

    /**
     * Marca un turno como completado (el paciente asistió).
     * Implementa RF9 - CU 4.1: Gestionar Asistencia al Turno.
     *
     * Validaciones:
     * - El turno debe existir
     * - El turno debe estar en estado SCHEDULED
     * - El practicante debe ser el responsable de la atención
     *
     * @param appointmentId ID del turno a marcar como completado
     * @param practitionerUser Usuario practicante (obtenido del contexto de seguridad)
     * @return El Appointment actualizado con estado COMPLETED
     */
    Appointment markAppointmentAsCompleted(Long appointmentId, User practitionerUser);

    /**
     * Marca un turno como "ausente" (el paciente no asistió).
     * Implementa RF9 - CU 4.1: Gestionar Asistencia al Turno.
     *
     * Tras la transición a NO_SHOW se evalúa la regla de funnel tracking:
     * si la Atención padre se queda sin trabajo clínico ni próximos turnos,
     * se cierra automáticamente como CANCELLED.
     *
     * @param appointmentId ID del turno a marcar como ausente
     * @param practitionerUser Usuario practicante (obtenido del contexto de seguridad)
     * @return El Appointment actualizado con estado NO_SHOW
     */
    Appointment markAppointmentAsNoShow(Long appointmentId, User practitionerUser);

    /**
     * Cancela un turno por iniciativa del PACIENTE.
     *
     * Reglas:
     * - El motivo es OPCIONAL: el paciente puede cancelar sin justificarse,
     *   aunque cualquier texto recibido se persiste para nutrir el funnel.
     * - El turno debe estar en estado SCHEDULED.
     * - El paciente autenticado debe ser el dueño de la Atención asociada.
     * - Tras cancelar se evalúa el funnel tracking: si la Atención queda
     *   sin trabajo realizado ni próximos turnos, se cierra como CANCELLED.
     *
     * @param appointmentId ID del turno
     * @param reason Motivo opcional (puede ser null/blank)
     * @param patientUser Usuario paciente autenticado
     * @return El Appointment actualizado con estado CANCELLED
     */
    Appointment cancelAppointmentByPatient(Long appointmentId, String reason, User patientUser);

    /**
     * Cancela un turno por iniciativa del PRACTICANTE.
     *
     * Reglas:
     * - El motivo es OBLIGATORIO: aporta auditoría académica y se muestra al paciente.
     * - El turno debe estar en estado SCHEDULED.
     * - El practicante autenticado debe ser el dueño de la Atención asociada.
     * - Tras cancelar se evalúa el funnel tracking de la Atención padre.
     *
     * @param appointmentId ID del turno
     * @param reason Motivo obligatorio
     * @param practitionerUser Usuario practicante autenticado
     * @return El Appointment actualizado con estado CANCELLED
     */
    Appointment cancelAppointmentByPractitioner(Long appointmentId, String reason, User practitionerUser);
}
