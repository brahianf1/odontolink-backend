package site.utnpf.odontolink.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.utnpf.odontolink.application.port.in.IAppointmentUseCase;
import site.utnpf.odontolink.application.port.in.IAttentionUseCase;
import site.utnpf.odontolink.application.port.in.IAuthUseCase;
import site.utnpf.odontolink.application.port.in.IOfferedTreatmentUseCase;
import site.utnpf.odontolink.application.port.in.IPatientRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.IPractitionerRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.ITreatmentUseCase;
import site.utnpf.odontolink.application.port.out.ITokenProvider;
import site.utnpf.odontolink.application.service.AppointmentService;
import site.utnpf.odontolink.application.service.AttentionService;
import site.utnpf.odontolink.application.service.AuthService;
import site.utnpf.odontolink.application.service.OfferedTreatmentService;
import site.utnpf.odontolink.application.service.PatientRegistrationService;
import site.utnpf.odontolink.application.service.PractitionerRegistrationService;
import site.utnpf.odontolink.application.service.SupervisorRegistrationService;
import site.utnpf.odontolink.application.service.TreatmentService;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.AvailabilitySlotRepository;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.ProgressNoteRepository;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;
import site.utnpf.odontolink.domain.repository.TreatmentRepository;
import site.utnpf.odontolink.domain.repository.UserRepository;
import site.utnpf.odontolink.domain.service.AppointmentBookingService;
import site.utnpf.odontolink.domain.service.AttentionPolicyService;
import site.utnpf.odontolink.domain.service.AvailabilityGenerationService;
import site.utnpf.odontolink.domain.service.OfferedTreatmentDomainService;

/**
 * Configuración de Beans para la capa de aplicación.
 * Define explícitamente los beans de los casos de uso (puertos de entrada)
 * y servicios de dominio siguiendo los principios de Arquitectura Hexagonal.
 *
 * Incluye beans para:
 * - Autenticación y registro de usuarios
 * - Catálogo de tratamientos
 * - Reserva de turnos (CU-008)
 *
 * @author OdontoLink Team
 */
@Configuration
public class BeanConfiguration {

    /**
     * Bean para el caso de uso de autenticación.
     * Expone la interfaz IAuthUseCase implementada por AuthService.
     * Usa ITokenProvider (puerto) para desacoplar de JWT.
     */
    @Bean
    public IAuthUseCase authUseCase(AuthenticationManager authenticationManager,
                                    ITokenProvider tokenProvider,
                                    UserRepository userRepository) {
        return new AuthService(authenticationManager, tokenProvider, userRepository);
    }

    /**
     * Bean para el caso de uso de registro de pacientes.
     * Expone la interfaz IPatientRegistrationUseCase implementada por PatientRegistrationService.
     */
    @Bean
    public IPatientRegistrationUseCase patientRegistrationUseCase(UserRepository userRepository,
                                                                  PatientRepository patientRepository,
                                                                  PasswordEncoder passwordEncoder) {
        return new PatientRegistrationService(userRepository, patientRepository, passwordEncoder);
    }

    /**
     * Bean para el caso de uso de registro de practicantes.
     * Expone la interfaz IPractitionerRegistrationUseCase implementada por PractitionerRegistrationService.
     */
    @Bean
    public IPractitionerRegistrationUseCase practitionerRegistrationUseCase(UserRepository userRepository,
                                                                            PractitionerRepository practitionerRepository,
                                                                            PasswordEncoder passwordEncoder) {
        return new PractitionerRegistrationService(userRepository, practitionerRepository, passwordEncoder);
    }

    /**
     * Bean para el caso de uso de registro de supervisores/docentes.
     * Expone la interfaz ISupervisorRegistrationUseCase implementada por SupervisorRegistrationService.
     */
    @Bean
    public ISupervisorRegistrationUseCase supervisorRegistrationUseCase(UserRepository userRepository,
                                                                        SupervisorRepository supervisorRepository,
                                                                        PasswordEncoder passwordEncoder) {
        return new SupervisorRegistrationService(userRepository, supervisorRepository, passwordEncoder);
    }

    /**
     * Bean para el servicio de dominio de OfferedTreatment.
     * Este servicio contiene las reglas de negocio puras (el "Rulebook").
     */
    @Bean
    public OfferedTreatmentDomainService offeredTreatmentDomainService(OfferedTreatmentRepository offeredTreatmentRepository) {
        return new OfferedTreatmentDomainService(offeredTreatmentRepository);
    }

    /**
     * Bean para el caso de uso de gestión del catálogo maestro de tratamientos.
     * Expone la interfaz ITreatmentUseCase implementada por TreatmentService.
     */
    @Bean
    public ITreatmentUseCase treatmentUseCase(TreatmentRepository treatmentRepository) {
        return new TreatmentService(treatmentRepository);
    }

    /**
     * Bean para el caso de uso de gestión del catálogo personal de tratamientos del practicante.
     * Expone la interfaz IOfferedTreatmentUseCase implementada por OfferedTreatmentService.
     * Depende del servicio de dominio para aplicar reglas de negocio.
     */
    @Bean
    public IOfferedTreatmentUseCase offeredTreatmentUseCase(OfferedTreatmentRepository offeredTreatmentRepository,
                                                            PractitionerRepository practitionerRepository,
                                                            TreatmentRepository treatmentRepository,
                                                            OfferedTreatmentDomainService domainService) {
        return new OfferedTreatmentService(
                offeredTreatmentRepository,
                practitionerRepository,
                treatmentRepository,
                domainService
        );
    }

    /**
     * Bean para el servicio de dominio de AppointmentBooking.
     * Este es el "Rulebook" que contiene las reglas de negocio críticas para reservar turnos:
     * - Validación de oferta de tratamiento
     * - Validación de disponibilidad horaria
     * - Validación de conflictos (paciente y practicante)
     * - Creación atómica de Attention + Appointment
     *
     * Este servicio opera exclusivamente con POJOs de dominio.
     */
    @Bean
    public AppointmentBookingService appointmentBookingService(
            OfferedTreatmentRepository offeredTreatmentRepository,
            AvailabilitySlotRepository availabilitySlotRepository,
            AppointmentRepository appointmentRepository,
            AttentionRepository attentionRepository) {
        return new AppointmentBookingService(
                offeredTreatmentRepository,
                availabilitySlotRepository,
                appointmentRepository,
                attentionRepository
        );
    }

    /**
     * Bean para el servicio de dominio de AvailabilityGeneration.
     * Este es el servicio de dominio que implementa el algoritmo de inventario dinámico:
     * - Genera slots teóricos basados en la duración del servicio
     * - Filtra slots que colisionan con turnos ya reservados
     * - Devuelve solo los slots realmente disponibles
     *
     * Este servicio opera exclusivamente con POJOs de dominio.
     */
    @Bean
    public AvailabilityGenerationService availabilityGenerationService(
            AppointmentRepository appointmentRepository,
            OfferedTreatmentRepository offeredTreatmentRepository) {
        return new AvailabilityGenerationService(
                appointmentRepository,
                offeredTreatmentRepository
        );
    }

    /**
     * Bean para el caso de uso de gestión de turnos.
     * Expone la interfaz IAppointmentUseCase implementada por AppointmentService.
     *
     * Implementa el CU-008: "Reservar Turno" y funcionalidades relacionadas:
     * - Reservar primer turno (crea Attention + Appointment atómicamente)
     * - Ver catálogo público de tratamientos ofrecidos
     * - Consultar turnos agendados (paciente y practicante)
     * - Obtener slots disponibles (inventario dinámico)
     *
     * Este servicio es el orquestador transaccional que:
     * 1. Carga entidades desde repositorios
     * 2. Delega al AppointmentBookingService (dominio) para aplicar reglas de negocio
     * 3. Delega al AvailabilityGenerationService para calcular inventario dinámico
     * 4. Persiste cambios de forma transaccional
     */
    @Bean
    public IAppointmentUseCase appointmentUseCase(
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            AttentionRepository attentionRepository,
            OfferedTreatmentRepository offeredTreatmentRepository,
            AppointmentBookingService appointmentBookingService,
            AvailabilityGenerationService availabilityGenerationService) {
        return new AppointmentService(
                patientRepository,
                appointmentRepository,
                attentionRepository,
                offeredTreatmentRepository,
                appointmentBookingService,
                availabilityGenerationService
        );
    }

    /**
     * Bean para el servicio de dominio de AttentionPolicy.
     * Este es el "Rulebook" que contiene las reglas de negocio complejas para finalizar casos:
     * - Validación de que no existan turnos futuros agendados
     * - Validación de que todos los turnos pasados estén marcados
     * - Delegación al POJO para el cambio de estado
     *
     * Este servicio opera exclusivamente con POJOs de dominio.
     * Implementa RF10, RF19 - CU 4.4: Finalizar Caso Clínico.
     */
    @Bean
    public AttentionPolicyService attentionPolicyService(AppointmentRepository appointmentRepository) {
        return new AttentionPolicyService(appointmentRepository);
    }

    /**
     * Bean para el caso de uso de gestión de atenciones (casos clínicos).
     * Expone la interfaz IAttentionUseCase implementada por AttentionService.
     *
     * Implementa los casos de uso de la Fase 4 - Trazabilidad del Caso Clínico:
     * - CU 4.2: Registrar Evolución (RF11)
     * - CU 4.4: Finalizar Caso Clínico (RF10, RF19)
     *
     * Este servicio es el orquestador transaccional que:
     * 1. Carga entidades desde repositorios
     * 2. Valida permisos y autorización
     * 3. Delega al AttentionPolicyService (dominio) para aplicar reglas de negocio complejas
     * 4. Delega a los POJOs para lógica de negocio simple
     * 5. Persiste cambios de forma transaccional
     */
    @Bean
    public IAttentionUseCase attentionUseCase(
            AttentionRepository attentionRepository,
            ProgressNoteRepository progressNoteRepository,
            AttentionPolicyService attentionPolicyService) {
        return new AttentionService(
                attentionRepository,
                progressNoteRepository,
                attentionPolicyService
        );
    }
}
