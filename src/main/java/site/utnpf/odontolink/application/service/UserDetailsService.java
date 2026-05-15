package site.utnpf.odontolink.application.service;

import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IUserDetailsUseCase;
import site.utnpf.odontolink.application.port.in.MyDetailsView;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;
import site.utnpf.odontolink.domain.repository.UserRepository;

/**
 * Caso de uso para la lectura/actualizacion de datos rol-especificos del
 * autoservicio (RF06 extension).
 *
 * <p>Implementa el contrato discutido con el equipo de frontend el 2026-05-15:
 * el PATIENT puede editar sus datos clinicos (obra social, grupo sanguineo);
 * el SUPERVISOR puede editar su especialidad; el PRACTITIONER y el ADMIN
 * solo leen. Las invariantes "studentId/studyYear/employeeId son inmutables"
 * se encarnan aqui: simplemente no exponemos un endpoint que los modifique.
 */
@Transactional
public class UserDetailsService implements IUserDetailsUseCase {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final SupervisorRepository supervisorRepository;

    public UserDetailsService(UserRepository userRepository,
                              PatientRepository patientRepository,
                              PractitionerRepository practitionerRepository,
                              SupervisorRepository supervisorRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.practitionerRepository = practitionerRepository;
        this.supervisorRepository = supervisorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MyDetailsView getMyDetails(Long userId) {
        User user = loadUser(userId);
        Role role = user.getRole();
        String roleName = role != null ? role.name() : null;

        return switch (role) {
            case ROLE_PATIENT -> {
                Patient patient = patientRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Patient", "userId", String.valueOf(userId)));
                yield MyDetailsView.forPatient(userId, roleName,
                        patient.getHealthInsurance(), patient.getBloodType());
            }
            case ROLE_PRACTITIONER -> {
                Practitioner practitioner = practitionerRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Practitioner", "userId", String.valueOf(userId)));
                yield MyDetailsView.forPractitioner(userId, roleName,
                        practitioner.getStudentId(), practitioner.getStudyYear());
            }
            case ROLE_SUPERVISOR -> {
                Supervisor supervisor = supervisorRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Supervisor", "userId", String.valueOf(userId)));
                yield MyDetailsView.forSupervisor(userId, roleName,
                        supervisor.getSpecialty(), supervisor.getEmployeeId());
            }
            case ROLE_ADMIN -> MyDetailsView.forAdmin(userId, roleName);
            default -> throw new InvalidBusinessRuleException(
                    "Rol no soportado para detalles: " + role);
        };
    }

    @Override
    public void updatePatientDetails(Long userId,
                                     JsonNullable<String> healthInsurance,
                                     JsonNullable<String> bloodType) {
        User user = loadUser(userId);
        if (user.getRole() != Role.ROLE_PATIENT) {
            throw new InvalidBusinessRuleException(
                    "Esta operacion solo aplica a usuarios con rol PATIENT.");
        }

        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient", "userId", String.valueOf(userId)));

        applyOptional(healthInsurance, patient::setHealthInsurance);
        applyOptional(bloodType, value -> patient.setBloodType(value == null ? null : value.toUpperCase()));

        patientRepository.save(patient);
    }

    @Override
    public void updateSupervisorDetails(Long userId, JsonNullable<String> specialty) {
        User user = loadUser(userId);
        if (user.getRole() != Role.ROLE_SUPERVISOR) {
            throw new InvalidBusinessRuleException(
                    "Esta operacion solo aplica a usuarios con rol SUPERVISOR.");
        }

        // specialty es @NotBlank en el registro: mantenemos esa invariante al
        // editar. Si el supervisor envia el campo presente pero con null o
        // cadena vacia (intencion "limpiar"), rechazamos. Para no modificarlo,
        // el FE debe omitir el campo del payload (semantica PATCH).
        if (specialty != null && specialty.isPresent()) {
            String raw = specialty.get();
            if (raw == null || raw.trim().isEmpty()) {
                throw new InvalidBusinessRuleException(
                        "La especialidad no puede quedar vacia.");
            }
        }

        Supervisor supervisor = supervisorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supervisor", "userId", String.valueOf(userId)));

        applyOptional(specialty, supervisor::setSpecialty);

        supervisorRepository.save(supervisor);
    }

    /**
     * Aplica un campo PATCH: si viene definido (incluso con null o vacio),
     * dispara el consumer; si viene undefined, no toca el valor. La
     * normalizacion "vacio = null" se aplica antes para uniformizar.
     */
    private void applyOptional(JsonNullable<String> value, java.util.function.Consumer<String> setter) {
        if (value == null || !value.isPresent()) {
            return;
        }
        String raw = value.get();
        String normalized;
        if (raw == null) {
            normalized = null;
        } else {
            String trimmed = raw.trim();
            normalized = trimmed.isEmpty() ? null : trimmed;
        }
        setter.accept(normalized);
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario", "id", String.valueOf(userId)));
    }
}
