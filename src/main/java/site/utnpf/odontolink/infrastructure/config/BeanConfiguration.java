package site.utnpf.odontolink.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.utnpf.odontolink.application.port.in.IAuthUseCase;
import site.utnpf.odontolink.application.port.in.IPatientRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.IPractitionerRegistrationUseCase;
import site.utnpf.odontolink.application.port.out.ITokenProvider;
import site.utnpf.odontolink.application.service.AuthService;
import site.utnpf.odontolink.application.service.PatientRegistrationService;
import site.utnpf.odontolink.application.service.PractitionerRegistrationService;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.UserRepository;

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
}
