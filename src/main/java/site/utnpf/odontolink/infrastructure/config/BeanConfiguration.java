package site.utnpf.odontolink.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.utnpf.odontolink.application.port.in.IAppointmentUseCase;
import site.utnpf.odontolink.application.port.in.IAttentionUseCase;
import site.utnpf.odontolink.application.port.in.IAuthUseCase;
import site.utnpf.odontolink.application.port.in.IChatUseCase;
import site.utnpf.odontolink.application.port.in.IFeedbackUseCase;
import site.utnpf.odontolink.application.port.in.IOfferedTreatmentUseCase;
import site.utnpf.odontolink.application.port.in.IPatientRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.IPractitionerRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorUseCase;
import site.utnpf.odontolink.application.port.in.ITreatmentUseCase;
import site.utnpf.odontolink.application.port.out.ITokenProvider;
import site.utnpf.odontolink.application.service.AppointmentService;
import site.utnpf.odontolink.application.service.AttentionService;
import site.utnpf.odontolink.application.service.AuthService;
import site.utnpf.odontolink.application.service.ChatService;
import site.utnpf.odontolink.application.service.FeedbackService;
import site.utnpf.odontolink.application.service.OfferedTreatmentService;
import site.utnpf.odontolink.application.service.PatientRegistrationService;
import site.utnpf.odontolink.application.service.PractitionerRegistrationService;
import site.utnpf.odontolink.application.service.SupervisorRegistrationService;
import site.utnpf.odontolink.application.service.SupervisorService;
import site.utnpf.odontolink.application.service.TreatmentService;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.AvailabilitySlotRepository;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.ProgressNoteRepository;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;
import site.utnpf.odontolink.domain.repository.TreatmentRepository;
import site.utnpf.odontolink.domain.repository.UserRepository;
import site.utnpf.odontolink.domain.repository.ChatSessionRepository;
import site.utnpf.odontolink.domain.repository.ChatMessageRepository;
import site.utnpf.odontolink.domain.service.AppointmentBookingService;
import site.utnpf.odontolink.domain.service.AttentionPolicyService;
import site.utnpf.odontolink.domain.service.AvailabilityGenerationService;
import site.utnpf.odontolink.domain.service.ChatPolicyService;
import site.utnpf.odontolink.domain.service.FeedbackPolicyService;
import site.utnpf.odontolink.domain.service.OfferedTreatmentDomainService;
import site.utnpf.odontolink.domain.service.SupervisorPolicyService;

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
            AttentionRepository attentionRepository,
            ChatSessionRepository chatSessionRepository) {
        return new AppointmentBookingService(
                offeredTreatmentRepository,
                availabilitySlotRepository,
                appointmentRepository,
                attentionRepository,
                chatSessionRepository
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

    /**
     * Bean para el servicio de dominio de SupervisorPolicy.
     * Este es el "Rulebook" que contiene las reglas de negocio de vinculación académica:
     * - Gestión de la relación N-a-N entre Supervisor y Practitioner
     * - Validación de vínculos duplicados
     * - Validación de acceso del supervisor a información de practicantes
     * - Mantenimiento de consistencia bidireccional de la relación
     *
     * Este servicio opera exclusivamente con POJOs de dominio.
     * Implementa RF22, RF37, RF40 - CU 7.1, CU 7.2.
     */
    @Bean
    public SupervisorPolicyService supervisorPolicyService() {
        return new SupervisorPolicyService();
    }

    /**
     * Bean para el caso de uso de gestión de supervisión académica.
     * Expone la interfaz ISupervisorUseCase implementada por SupervisorService.
     *
     * Implementa los casos de uso de vinculación académica:
     * - CU 7.1: Vincular Practicante (RF22, RF37)
     * - CU 7.2: Desvincular Practicante
     * - CU 7.3: Visualizar y Buscar Practicantes (RF35, RF38)
     *
     * Este servicio es el orquestador transaccional que:
     * 1. Carga entidades desde repositorios
     * 2. Delega al SupervisorPolicyService (dominio) para aplicar reglas de negocio
     * 3. Persiste cambios de forma transaccional
     */
    @Bean
    public ISupervisorUseCase supervisorUseCase(
            SupervisorRepository supervisorRepository,
            PractitionerRepository practitionerRepository,
            SupervisorPolicyService supervisorPolicyService) {
        return new SupervisorService(
                supervisorRepository,
                practitionerRepository,
                supervisorPolicyService
        );
    }

    /**
     * Bean para el servicio de dominio de FeedbackPolicy.
     * Este es el "Rulebook" que contiene las reglas de negocio complejas del sistema de feedback:
     * - Validación de que la atención esté finalizada (COMPLETED)
     * - Validación de pertenencia (paciente o practicante)
     * - Validación de unicidad (evitar calificaciones duplicadas - RF23)
     * - Validación de privacidad (acceso segmentado por rol - RF24)
     *
     * Este servicio opera exclusivamente con POJOs de dominio.
     * Implementa RF21, RF22, RF23, RF24 - CU-009, CU-016, CU-010.
     */
    @Bean
    public FeedbackPolicyService feedbackPolicyService(FeedbackRepository feedbackRepository) {
        return new FeedbackPolicyService(feedbackRepository);
    }

    /**
     * Bean para el caso de uso de gestión de feedback.
     * Expone la interfaz IFeedbackUseCase implementada por FeedbackService.
     *
     * Implementa los casos de uso del sistema de feedback bidireccional:
     * - CU-009: Calificar Paciente (RF21)
     * - CU-016: Calificar Practicante (RF22)
     * - CU-010: Visualizar Feedback (RF24, RF25, RF40)
     *
     * Este servicio es el orquestador transaccional que:
     * 1. Carga entidades desde repositorios
     * 2. Valida permisos y autorización
     * 3. Delega al FeedbackPolicyService y SupervisorPolicyService (dominio) para aplicar reglas de negocio
     * 4. Persiste cambios de forma transaccional
     */
    @Bean
    public IFeedbackUseCase feedbackUseCase(
            FeedbackRepository feedbackRepository,
            AttentionRepository attentionRepository,
            FeedbackPolicyService feedbackPolicyService,
            SupervisorRepository supervisorRepository,
            PractitionerRepository practitionerRepository,
            SupervisorPolicyService supervisorPolicyService) {
        return new FeedbackService(
                feedbackRepository,
                attentionRepository,
                feedbackPolicyService,
                supervisorRepository,
                practitionerRepository,
                supervisorPolicyService
        );
    }

    /**
     * Bean para el servicio de dominio de ChatPolicy.
     * Este es el "Rulebook" que contiene las reglas de negocio del sistema de chat:
     * - Validación de pertenencia a la sesión de chat
     * - Validación de permisos para enviar y ver mensajes
     * - Aplicación de las políticas de privacidad del chat (RF26, RF27)
     *
     * Este servicio opera exclusivamente con POJOs de dominio.
     */
    @Bean
    public ChatPolicyService chatPolicyService() {
        return new ChatPolicyService();
    }

    /**
     * Bean para el caso de uso de gestión de chat interno.
     * Expone la interfaz IChatUseCase implementada por ChatService.
     *
     * Implementa los casos de uso del sistema de chat interno:
     * - CU 6.1: Obtener Lista de Sesiones de Chat (El "Inbox")
     * - CU 6.2: Enviar un Mensaje (RF26)
     * - CU 6.3: Obtener Mensajes (Polling RESTful)
     *
     * Este servicio es el orquestador transaccional que:
     * 1. Carga entidades desde repositorios
     * 2. Valida permisos y autorización
     * 3. Delega al ChatPolicyService (dominio) para aplicar reglas de negocio
     * 4. Persiste cambios de forma transaccional
     *
     * El sistema de chat utiliza Simple Polling (REST) en lugar de WebSockets
     * para mantener la simplicidad y robustez de la arquitectura.
     */
    @Bean
    public IChatUseCase chatUseCase(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            PatientRepository patientRepository,
            PractitionerRepository practitionerRepository,
            ChatPolicyService chatPolicyService) {
        return new ChatService(
                chatSessionRepository,
                chatMessageRepository,
                patientRepository,
                practitionerRepository,
                chatPolicyService
        );
    }
}
