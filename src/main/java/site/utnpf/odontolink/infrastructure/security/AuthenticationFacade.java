package site.utnpf.odontolink.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.UserRepository;

/**
 * Fachada de autenticación que encapsula la lógica de obtención de información
 * del usuario autenticado desde el contexto de seguridad de Spring.
 * 
 * Este componente de infraestructura permite desacoplar los controladores
 * de la lógica específica de Spring Security, siguiendo el principio de
 * responsabilidad única y facilitando el testing.
 * 
 * @author OdontoLink Team
 */
@Component
public class AuthenticationFacade {

    private final UserRepository userRepository;
    private final PractitionerRepository practitionerRepository;
    private final PatientRepository patientRepository;

    public AuthenticationFacade(UserRepository userRepository,
                               PractitionerRepository practitionerRepository,
                               PatientRepository patientRepository) {
        this.userRepository = userRepository;
        this.practitionerRepository = practitionerRepository;
        this.patientRepository = patientRepository;
    }

    /**
     * Obtiene el Authentication actual del contexto de seguridad.
     * 
     * @return El objeto Authentication de Spring Security
     */
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Obtiene el email del usuario autenticado actualmente.
     * 
     * @return El email del usuario autenticado
     */
    public String getAuthenticatedUserEmail() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername(); // En nuestro sistema, username = email
        }
        throw new IllegalStateException("No hay un usuario autenticado en el contexto de seguridad");
    }

    /**
     * Obtiene el User completo del usuario autenticado.
     * 
     * @return La entidad User del usuario autenticado
     * @throws ResourceNotFoundException si el usuario no se encuentra en la base de datos
     */
    public User getAuthenticatedUser() {
        String email = getAuthenticatedUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Obtiene el ID del Practitioner asociado al usuario autenticado.
     * 
     * Este método es útil para endpoints que requieren el practitionerId
     * y están protegidos con @PreAuthorize("hasRole('PRACTITIONER')").
     * 
     * @return El ID del Practitioner
     * @throws ResourceNotFoundException si el usuario no tiene un perfil de practicante asociado
     */
    public Long getAuthenticatedPractitionerId() {
        User user = getAuthenticatedUser();
        Practitioner practitioner = practitionerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Practitioner", "userId", user.getId().toString()));
        return practitioner.getId();
    }

    /**
     * Obtiene el ID del Patient asociado al usuario autenticado.
     *
     * Este método es útil para endpoints que requieren el patientId
     * y están protegidos con @PreAuthorize("hasRole('PATIENT')").
     *
     * Soporta el CU-008: "Reservar Turno".
     *
     * @return El ID del Patient
     * @throws ResourceNotFoundException si el usuario no tiene un perfil de paciente asociado
     */
    public Long getAuthenticatedPatientId() {
        User user = getAuthenticatedUser();
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient", "userId", user.getId().toString()));
        return patient.getId();
    }
}
