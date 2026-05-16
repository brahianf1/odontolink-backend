package site.utnpf.odontolink.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.utnpf.odontolink.application.port.in.IAdminUserManagementUseCase;
import site.utnpf.odontolink.application.port.in.IAppointmentUseCase;
import site.utnpf.odontolink.application.port.in.IAttentionUseCase;
import site.utnpf.odontolink.application.port.in.IAuthUseCase;
import site.utnpf.odontolink.application.port.in.IChatUseCase;
import site.utnpf.odontolink.application.port.in.IFeedbackUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorFeedbackDashboardUseCase;
import site.utnpf.odontolink.application.port.in.IInstitutionalSettingsUseCase;
import site.utnpf.odontolink.application.port.in.IOfferedTreatmentUseCase;
import site.utnpf.odontolink.application.port.in.ISearchOfferedTreatmentsUseCase;
import site.utnpf.odontolink.application.port.in.IPasswordResetUseCase;
import site.utnpf.odontolink.application.port.in.IPatientRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.IPractitionerRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.IProfilePictureUseCase;
import site.utnpf.odontolink.application.port.in.IProfileUseCase;
import site.utnpf.odontolink.application.port.in.IUserDetailsUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorAttentionUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorUseCase;
import site.utnpf.odontolink.application.port.in.ITreatmentUseCase;
import site.utnpf.odontolink.application.port.out.IEmailSenderPort;
import site.utnpf.odontolink.application.port.out.IObjectStoragePort;
import site.utnpf.odontolink.application.port.out.ITokenProvider;
import site.utnpf.odontolink.application.service.AdminUserManagementService;
import site.utnpf.odontolink.application.service.AppointmentService;
import site.utnpf.odontolink.application.service.AttentionService;
import site.utnpf.odontolink.application.service.AuthService;
import site.utnpf.odontolink.application.service.ChatService;
import site.utnpf.odontolink.application.service.FeedbackService;
import site.utnpf.odontolink.application.service.SupervisorFeedbackDashboardService;
import site.utnpf.odontolink.application.service.InstitutionalSettingsService;
import site.utnpf.odontolink.application.service.OfferedTreatmentService;
import site.utnpf.odontolink.application.service.SearchOfferedTreatmentsService;
import site.utnpf.odontolink.application.service.PasswordResetService;
import site.utnpf.odontolink.application.service.PatientRegistrationService;
import site.utnpf.odontolink.application.service.PractitionerRegistrationService;
import site.utnpf.odontolink.application.service.ProfilePictureService;
import site.utnpf.odontolink.application.service.ProfileService;
import site.utnpf.odontolink.application.service.UserDetailsService;
import site.utnpf.odontolink.application.service.SupervisorAttentionService;
import site.utnpf.odontolink.application.service.SupervisorRegistrationService;
import site.utnpf.odontolink.application.service.SupervisorService;
import site.utnpf.odontolink.application.service.TreatmentService;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.AvailabilitySlotRepository;
import site.utnpf.odontolink.domain.repository.FeedbackRepository;
import site.utnpf.odontolink.domain.repository.InstitutionalSettingsRepository;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;
import site.utnpf.odontolink.domain.repository.PasswordResetTokenRepository;
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
import site.utnpf.odontolink.domain.service.slotstrategy.DynamicDurationSlotStrategy;
import site.utnpf.odontolink.domain.service.slotstrategy.FixedIntervalSlotStrategy;
import site.utnpf.odontolink.domain.service.slotstrategy.SlotGenerationStrategy;
import site.utnpf.odontolink.infrastructure.config.ratelimit.RateLimitRegistry;

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
     * Bean para el caso de uso de recuperación de contraseña (RF04).
     *
     * El TTL del token se inyecta desde {@code application.properties} para
     * permitir ajustar la ventana de vigencia por ambiente sin recompilar.
     * El valor por defecto (30 minutos) sigue la recomendación de OWASP para
     * tokens de un solo uso entregados por email.
     */
    @Bean
    public IPasswordResetUseCase passwordResetUseCase(UserRepository userRepository,
                                                      PasswordResetTokenRepository passwordResetTokenRepository,
                                                      IEmailSenderPort emailSenderPort,
                                                      PasswordEncoder passwordEncoder,
                                                      RateLimitRegistry rateLimitRegistry,
                                                      @Value("${odontolink.password-reset.token-ttl-minutes:30}") long tokenTtlMinutes) {
        return new PasswordResetService(
                userRepository,
                passwordResetTokenRepository,
                emailSenderPort,
                passwordEncoder,
                rateLimitRegistry,
                tokenTtlMinutes
        );
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
                                                            AttentionRepository attentionRepository,
                                                            OfferedTreatmentDomainService domainService) {
        return new OfferedTreatmentService(
                offeredTreatmentRepository,
                practitionerRepository,
                treatmentRepository,
                attentionRepository,
                domainService
        );
    }

    /**
     * Bean para el motor de búsqueda paginada del catálogo público (RF09).
     *
     * Se expone como caso de uso independiente para respetar Segregación de
     * Interfaces: el flujo de búsqueda del paciente no comparte dependencias
     * con la administración del catálogo del practicante ni con la reserva
     * de turnos.
     */
    @Bean
    public ISearchOfferedTreatmentsUseCase searchOfferedTreatmentsUseCase(
            OfferedTreatmentRepository offeredTreatmentRepository) {
        return new SearchOfferedTreatmentsService(offeredTreatmentRepository);
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
            ChatSessionRepository chatSessionRepository,
            InstitutionalSettingsRepository institutionalSettingsRepository) {
        return new AppointmentBookingService(
                offeredTreatmentRepository,
                availabilitySlotRepository,
                appointmentRepository,
                attentionRepository,
                chatSessionRepository,
                institutionalSettingsRepository
        );
    }

    /**
     * Bean para la estrategia de generación de slots.
     * Se configura mediante la propiedad odontolink.slot-strategy.
     * Valores posibles: FIXED (por defecto), DYNAMIC.
     */
    @Bean
    public SlotGenerationStrategy slotGenerationStrategy(
            @Value("${odontolink.slot-strategy:FIXED}") String strategyType) {

        if ("DYNAMIC".equalsIgnoreCase(strategyType)) {
            return new DynamicDurationSlotStrategy();
        }
        return new FixedIntervalSlotStrategy();
    }

    /**
     * Bean para el servicio de dominio de AvailabilityGeneration.
     * Este es el servicio de dominio que implementa el "Rulebook" del sistema de ofertas finitas.
     *
     * Implementa el algoritmo de inventario dinámico con tres validaciones secuenciales:
     * 1. Validación de límite temporal (offerStartDate - offerEndDate)
     * 2. Validación de límite de cupo (maxCompletedAttentions)
     * 3. Cálculo de inventario dinámico diario:
     *    - Genera slots teóricos basados en la duración del servicio
     *    - Filtra slots que colisionan con turnos ya reservados
     *    - Devuelve solo los slots realmente disponibles
     *
     * Filosofía "Lo que suceda primero": La oferta deja de estar disponible cuando se cumple
     * UNA de las condiciones (límite temporal o límite de cupo).
     *
     * Este servicio opera exclusivamente con POJOs de dominio.
     */
    @Bean
    public AvailabilityGenerationService availabilityGenerationService(
            AppointmentRepository appointmentRepository,
            OfferedTreatmentRepository offeredTreatmentRepository,
            AttentionRepository attentionRepository,
            SlotGenerationStrategy slotGenerationStrategy) {
        return new AvailabilityGenerationService(
                appointmentRepository,
                offeredTreatmentRepository,
                attentionRepository,
                slotGenerationStrategy
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
            AvailabilityGenerationService availabilityGenerationService,
            AttentionPolicyService attentionPolicyService) {
        return new AppointmentService(
                patientRepository,
                appointmentRepository,
                attentionRepository,
                offeredTreatmentRepository,
                appointmentBookingService,
                availabilityGenerationService,
                attentionPolicyService
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
    public AttentionPolicyService attentionPolicyService(AppointmentRepository appointmentRepository,
                                                         AttentionRepository attentionRepository) {
        return new AttentionPolicyService(appointmentRepository, attentionRepository);
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
     * Bean para el caso de uso del Módulo Académico de Auditoría Clínica (RF39).
     *
     * Implementa la potestad del docente para:
     *  - Listar y auditar atenciones de practicantes vinculados
     *  - Cerrar (COMPLETED) por autoridad académica una atención
     *
     * Se cablea contra repositorios de Supervisor, Practitioner, Attention y ProgressNote
     * y reutiliza los servicios de dominio existentes:
     *  - {@link SupervisorPolicyService} para el cerco de vinculación docente-alumno.
     *  - {@link AttentionPolicyService} para reaplicar las reglas clínicas de finalización
     *    sin duplicar lógica.
     */
    @Bean
    public ISupervisorAttentionUseCase supervisorAttentionUseCase(
            SupervisorRepository supervisorRepository,
            PractitionerRepository practitionerRepository,
            AttentionRepository attentionRepository,
            ProgressNoteRepository progressNoteRepository,
            SupervisorPolicyService supervisorPolicyService,
            AttentionPolicyService attentionPolicyService) {
        return new SupervisorAttentionService(
                supervisorRepository,
                practitionerRepository,
                attentionRepository,
                progressNoteRepository,
                supervisorPolicyService,
                attentionPolicyService
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
     * Bean para el caso de uso MICRO-contexto de feedback (atención puntual).
     * Expone la interfaz IFeedbackUseCase implementada por FeedbackService.
     *
     * Casos de uso orquestados:
     * - CU-009: Calificar Paciente (RF21)
     * - CU-016: Calificar Practicante (RF22)
     * - CU-010: Visualizar Feedback de una atención (RF24)
     *
     * El MACRO-contexto del docente (Panel de Supervisión RF25) se cablea en
     * {@link #supervisorFeedbackDashboardUseCase(FeedbackRepository, SupervisorRepository)}.
     */
    @Bean
    public IFeedbackUseCase feedbackUseCase(
            FeedbackRepository feedbackRepository,
            AttentionRepository attentionRepository,
            FeedbackPolicyService feedbackPolicyService) {
        return new FeedbackService(
                feedbackRepository,
                attentionRepository,
                feedbackPolicyService
        );
    }

    /**
     * Bean para el Panel Docente de Supervisión de Feedback (RF25).
     *
     * Es un caso de uso EXCLUSIVO del macro-contexto evaluador de desempeño:
     * análisis agregado del feedback recibido por los practicantes a cargo.
     * Se mantiene separado de {@link IFeedbackUseCase} (micro-contexto) por
     * Responsabilidad Única — distinto sombrero del docente, distintas reglas.
     *
     * Reutiliza el {@link SupervisorRepository} para resolver el cerco
     * docente-alumno sin duplicar la lógica de N-a-N ya validada en RF22/RF37/RF39.
     */
    @Bean
    public ISupervisorFeedbackDashboardUseCase supervisorFeedbackDashboardUseCase(
            FeedbackRepository feedbackRepository,
            SupervisorRepository supervisorRepository) {
        return new SupervisorFeedbackDashboardService(feedbackRepository, supervisorRepository);
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
            AppointmentRepository appointmentRepository,
            ChatPolicyService chatPolicyService) {
        return new ChatService(
                chatSessionRepository,
                chatMessageRepository,
                patientRepository,
                practitionerRepository,
                appointmentRepository,
                chatPolicyService
        );
    }

    /**
     * Bean para el caso de uso de gestión administrativa de usuarios (RF05).
     *
     * Se compone por delegación con los tres casos de uso de registro
     * existentes para evitar duplicar las validaciones de unicidad y la
     * creación de los perfiles específicos por rol. Cualquier nueva regla
     * que se incorpore al auto-registro se propagará automáticamente al
     * alta administrativa.
     */
    @Bean
    public IAdminUserManagementUseCase adminUserManagementUseCase(
            UserRepository userRepository,
            IPatientRegistrationUseCase patientRegistrationUseCase,
            IPractitionerRegistrationUseCase practitionerRegistrationUseCase,
            ISupervisorRegistrationUseCase supervisorRegistrationUseCase) {
        return new AdminUserManagementService(
                userRepository,
                patientRegistrationUseCase,
                practitionerRegistrationUseCase,
                supervisorRegistrationUseCase
        );
    }

    /**
     * Bean para el caso de uso de autoservicio del perfil del usuario (RF06).
     *
     * Se cablea contra el {@link UserRepository} para leer/persistir, contra
     * el {@link PasswordEncoder} ya definido en {@code SecurityConfig} para
     * verificar y reescribir el hash de la contraseña sin acoplarse a una
     * implementación concreta de hashing, y contra {@link ITokenProvider}
     * para emitir un JWT fresco tras el cambio de contraseña (Fase 2).
     */
    @Bean
    public IProfileUseCase profileUseCase(UserRepository userRepository,
                                          PasswordEncoder passwordEncoder,
                                          ITokenProvider tokenProvider,
                                          RateLimitRegistry rateLimitRegistry) {
        return new ProfileService(userRepository, passwordEncoder, tokenProvider, rateLimitRegistry);
    }

    /**
     * Bean para el use case de detalles rol-especificos (RF06 extension).
     *
     * Se cablea contra los tres repositorios de subtipos (Patient,
     * Practitioner, Supervisor) ya que cada PATCH actua sobre la tabla
     * correspondiente; el {@link UserRepository} se usa solo para resolver
     * el rol y validar que la operacion sea legitima.
     */
    @Bean
    public IUserDetailsUseCase userDetailsUseCase(UserRepository userRepository,
                                                  PatientRepository patientRepository,
                                                  PractitionerRepository practitionerRepository,
                                                  SupervisorRepository supervisorRepository) {
        return new UserDetailsService(userRepository, patientRepository,
                practitionerRepository, supervisorRepository);
    }

    /**
     * Bean para el use case de gestion de foto de perfil (RF06 extension).
     *
     * Los limites de tamanio, dimensiones y calidad JPEG se inyectan desde
     * {@code application.properties} para poder ajustarlos por ambiente
     * sin recompilar.
     */
    @Bean
    public IProfilePictureUseCase profilePictureUseCase(
            UserRepository userRepository,
            IObjectStoragePort objectStorage,
            @Value("${odontolink.profile-picture.max-bytes:2097152}") int maxBytes,
            @Value("${odontolink.profile-picture.target-size-px:512}") int targetSizePx,
            @Value("${odontolink.profile-picture.jpeg-quality:0.85}") double jpegQuality) {
        return new ProfilePictureService(
                userRepository,
                objectStorage,
                maxBytes,
                targetSizePx,
                jpegQuality
        );
    }

    /**
     * Bean para el caso de uso de configuración institucional (RF07).
     *
     * El servicio aplica las modificaciones de forma inmediata sobre la
     * fila singleton de la tabla {@code institutional_settings} y crea la
     * fila con valores por defecto en el primer acceso, garantizando que
     * los consumidores nunca vean un 404.
     */
    @Bean
    public IInstitutionalSettingsUseCase institutionalSettingsUseCase(
            InstitutionalSettingsRepository institutionalSettingsRepository) {
        return new InstitutionalSettingsService(institutionalSettingsRepository);
    }
}
