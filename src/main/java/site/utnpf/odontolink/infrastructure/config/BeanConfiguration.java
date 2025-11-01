package site.utnpf.odontolink.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.utnpf.odontolink.application.port.in.IAuthUseCase;
import site.utnpf.odontolink.application.port.in.IOfferedTreatmentUseCase;
import site.utnpf.odontolink.application.port.in.IPatientRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.IPractitionerRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.ITreatmentUseCase;
import site.utnpf.odontolink.application.port.out.ITokenProvider;
import site.utnpf.odontolink.application.service.AuthService;
import site.utnpf.odontolink.application.service.OfferedTreatmentService;
import site.utnpf.odontolink.application.service.PatientRegistrationService;
import site.utnpf.odontolink.application.service.PractitionerRegistrationService;
import site.utnpf.odontolink.application.service.SupervisorRegistrationService;
import site.utnpf.odontolink.application.service.TreatmentService;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;
import site.utnpf.odontolink.domain.repository.TreatmentRepository;
import site.utnpf.odontolink.domain.repository.UserRepository;
import site.utnpf.odontolink.domain.service.OfferedTreatmentDomainService;

/**
 * Configuración de Beans para la capa de aplicación.
 * Define explícitamente los beans de los casos de uso (puertos de entrada)
 * siguiendo los principios de Arquitectura Hexagonal.
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
}
