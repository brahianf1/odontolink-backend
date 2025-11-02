package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IAppointmentUseCase;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.*;
import site.utnpf.odontolink.domain.repository.*;
import site.utnpf.odontolink.domain.service.AppointmentBookingService;
import site.utnpf.odontolink.domain.service.AvailabilityGenerationService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de aplicación para la gestión de turnos (appointments).
 * Implementa el puerto de entrada IAppointmentUseCase siguiendo la Arquitectura Hexagonal.
 *
 * Este servicio es el orquestador transaccional del CU-008: Reservar Turno.
 * Su responsabilidad principal es coordinar:
 * 1. La carga de entidades de dominio desde los repositorios
 * 2. La delegación de lógica de negocio al servicio de dominio (AppointmentBookingService)
 * 3. La persistencia transaccional de los cambios
 *
 * Flujo de ejecución típico:
 * Controller -> AppointmentService (aquí) -> AppointmentBookingService (dominio) -> Repositories
 *
 * @Transactional asegura que toda la operación (crear Attention + Appointment) sea atómica.
 *
 * @author OdontoLink Team
 */
@Transactional
public class AppointmentService implements IAppointmentUseCase {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AttentionRepository attentionRepository;
    private final OfferedTreatmentRepository offeredTreatmentRepository;
    private final AppointmentBookingService appointmentBookingService;
    private final AvailabilityGenerationService availabilityGenerationService;

    public AppointmentService(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            AttentionRepository attentionRepository,
            OfferedTreatmentRepository offeredTreatmentRepository,
            AppointmentBookingService appointmentBookingService,
            AvailabilityGenerationService availabilityGenerationService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.attentionRepository = attentionRepository;
        this.offeredTreatmentRepository = offeredTreatmentRepository;
        this.appointmentBookingService = appointmentBookingService;
        this.availabilityGenerationService = availabilityGenerationService;
    }

    /**
     * Implementa el caso de uso principal CU-008: "Reservar Turno".
     *
     * Orquestación:
     * 1. Carga el Patient desde el repositorio
     * 2. Delega al AppointmentBookingService (servicio de dominio) para crear la Attention y el Appointment
     * 3. Persiste la Attention (y su Appointment hijo gracias a CascadeType.ALL) de forma transaccional
     *
     * @param patientId          ID del paciente autenticado
     * @param offeredTreatmentId ID de la oferta de tratamiento seleccionada
     * @param appointmentTime    Fecha y hora del turno solicitado
     * @return La Attention creada/actualizada con el nuevo Appointment
     * @throws ResourceNotFoundException si el paciente no existe
     */
    @Override
    public Attention scheduleFirstAppointment(Long patientId, Long offeredTreatmentId, LocalDateTime appointmentTime) {

        // Cargar el Patient desde el repositorio
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId.toString()));

        // Delegar al servicio de dominio para aplicar todas las reglas de negocio
        // El AppointmentBookingService se encarga de:
        // - Validar la oferta
        // - Validar disponibilidad horaria
        // - Validar conflictos
        // - Crear/actualizar la Attention con su Appointment
        Attention attention = appointmentBookingService.bookFirstAppointment(
                patient,
                offeredTreatmentId,
                appointmentTime
        );

        // Persistir la Attention (y su Appointment hijo gracias a CascadeType.ALL)
        // Esta es la operación transaccional que garantiza atomicidad
        return attentionRepository.save(attention);
    }

    /**
     * Obtiene los turnos agendados (SCHEDULED) de un paciente.
     * Usado para mostrar "Mis Turnos" en el panel del paciente.
     *
     * @param patientId ID del paciente
     * @return Lista de turnos con estado SCHEDULED
     */
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getUpcomingAppointmentsForPatient(Long patientId) {
        // Verificar que el paciente existe
        if (!patientRepository.findById(patientId).isPresent()) {
            throw new ResourceNotFoundException("Patient", "id", patientId.toString());
        }

        // Obtener turnos agendados (excluye cancelados, completados, etc.)
        return appointmentRepository.findByPatientIdAndStatus(patientId, AppointmentStatus.SCHEDULED);
    }

    /**
     * Obtiene los turnos agendados (SCHEDULED) de un practicante.
     * Usado para mostrar "Mis Turnos" en el panel del practicante.
     *
     * @param practitionerId ID del practicante
     * @return Lista de turnos con estado SCHEDULED
     */
    @Override
    @Transactional(readOnly = true)
    public List<Appointment> getUpcomingAppointmentsForPractitioner(Long practitionerId) {
        // Obtener turnos agendados del practicante
        return appointmentRepository.findByPractitionerIdAndStatus(practitionerId, AppointmentStatus.SCHEDULED);
    }

    /**
     * Obtiene el catálogo público de tratamientos ofrecidos.
     * Permite al paciente ver el catálogo antes de reservar un turno.
     *
     * Puede filtrar opcionalmente por un tipo de tratamiento específico.
     *
     * @param treatmentId ID del tratamiento para filtrar (opcional, puede ser null)
     * @return Lista de tratamientos ofrecidos disponibles
     */
    @Override
    @Transactional(readOnly = true)
    public List<OfferedTreatment> getAvailableOfferedTreatments(Long treatmentId) {
        if (treatmentId != null) {
            // Filtrar por tipo de tratamiento específico
            return offeredTreatmentRepository.findByTreatmentId(treatmentId);
        }

        // Retornar todos los tratamientos ofrecidos
        return offeredTreatmentRepository.findAll();
    }

    /**
     * Obtiene los slots de tiempo disponibles para un tratamiento ofrecido en una fecha específica.
     * Implementa el cálculo de inventario dinámico delegando al servicio de dominio.
     *
     * Orquestación:
     * 1. Carga el OfferedTreatment desde el repositorio
     * 2. Delega al AvailabilityGenerationService para calcular los slots disponibles
     * 3. Retorna la lista de slots (timestamps)
     *
     * @param offeredTreatmentId ID de la oferta de tratamiento
     * @param requestedDate Fecha para la cual se consultan los horarios
     * @return Lista de LocalDateTime con los slots disponibles
     * @throws ResourceNotFoundException si el OfferedTreatment no existe
     */
    @Override
    @Transactional(readOnly = true)
    public List<LocalDateTime> getAvailableSlots(Long offeredTreatmentId, LocalDate requestedDate) {
        // Cargar el OfferedTreatment desde el repositorio
        OfferedTreatment offeredTreatment = offeredTreatmentRepository.findById(offeredTreatmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "OfferedTreatment",
                        "id",
                        offeredTreatmentId.toString()
                ));

        // Delegar al servicio de dominio para calcular el inventario dinámico
        return availabilityGenerationService.generateAvailableSlots(offeredTreatment, requestedDate);
    }
}
