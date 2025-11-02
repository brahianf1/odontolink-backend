package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.domain.model.Attention;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Puerto de entrada (Input Port) para casos de uso relacionados con turnos (appointments).
 * Define las operaciones del CU-008: "Reservar Turno" y consultas relacionadas.
 *
 * Este puerto trabaja exclusivamente con objetos de dominio, manteniendo la capa de aplicación
 * independiente de la infraestructura. La conversión de DTOs a objetos de dominio se realiza
 * en los adaptadores de entrada (controladores con ayuda de mappers).
 *
 * @author OdontoLink Team
 */
public interface IAppointmentUseCase {

    /**
     * Reserva el primer turno para un paciente, creando atómicamente un "Caso Clínico" (Attention)
     * y el primer "Turno" (Appointment).
     *
     * Corresponde al CU-008: Reservar Turno.
     *
     * Este método orquesta:
     * 1. La carga de entidades necesarias (Patient, OfferedTreatment)
     * 2. La ejecución del servicio de dominio (AppointmentBookingService) que aplica todas las reglas de negocio
     * 3. La persistencia transaccional del agregado Attention con su Appointment hijo
     *
     * @param patientId          ID del paciente autenticado que solicita el turno
     * @param offeredTreatmentId ID de la oferta de tratamiento seleccionada del catálogo
     * @param appointmentTime    Fecha y hora exactas del turno solicitado
     * @return La Attention creada o actualizada con el nuevo Appointment
     */
    Attention scheduleFirstAppointment(Long patientId, Long offeredTreatmentId, LocalDateTime appointmentTime);

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
    List<site.utnpf.odontolink.domain.model.OfferedTreatment> getAvailableOfferedTreatments(Long treatmentId);

    /**
     * Obtiene los slots de tiempo disponibles para un tratamiento ofrecido en una fecha específica.
     * Este método implementa el cálculo de inventario dinámico.
     *
     * Corresponde a la nueva funcionalidad de "Ver Disponibilidad" del CU-008.
     *
     * El servicio calcula en tiempo real:
     * 1. Los slots teóricos basados en el bloque de disponibilidad y la duración del servicio
     * 2. Filtra los slots que colisionan con turnos ya reservados
     * 3. Devuelve solo los slots realmente disponibles para reservar
     *
     * @param offeredTreatmentId ID de la oferta de tratamiento
     * @param requestedDate Fecha para la cual se consultan los horarios disponibles
     * @return Lista de LocalDateTime con los slots disponibles (ej: [2025-01-15T08:00, 2025-01-15T08:30, ...])
     */
    List<LocalDateTime> getAvailableSlots(Long offeredTreatmentId, LocalDate requestedDate);
}
