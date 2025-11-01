package site.utnpf.odontolink.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IPatientRegistrationUseCase;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.PatientRepository;
import site.utnpf.odontolink.domain.repository.UserRepository;

import java.time.LocalDate;

/**
 * Servicio de aplicación para el registro de pacientes (CU-013).
 * Implementa el puerto de entrada IPatientRegistrationUseCase.
 * Extiende de BaseUserRegistrationService para reutilizar lógica común.
 *
 * Este servicio se encarga de:
 * - Coordinar el registro de un usuario con rol PATIENT
 * - Aplicar validaciones específicas de pacientes (si las hubiere en el futuro)
 * - Crear el perfil específico de Patient con healthInsurance y bloodType
 *
 * El bean se registra explícitamente en BeanConfiguration.
 */
@Transactional
public class PatientRegistrationService extends BaseUserRegistrationService
        implements IPatientRegistrationUseCase {

    private final PatientRepository patientRepository;

    // Variables de instancia para los datos específicos del paciente
    private String healthInsurance;
    private String bloodType;

    public PatientRegistrationService(UserRepository userRepository,
                                     PatientRepository patientRepository,
                                     PasswordEncoder passwordEncoder) {
        super(userRepository, passwordEncoder);
        this.patientRepository = patientRepository;
    }

    @Override
    public Patient registerPatient(String email, String password, String firstName, String lastName,
                                   String dni, String phone, LocalDate birthDate,
                                   String healthInsurance, String bloodType) {

        // Guardar datos específicos para uso en hooks
        this.healthInsurance = healthInsurance;
        this.bloodType = bloodType;

        // 1. Validar reglas comunes (email y DNI únicos)
        validateCommonRules(email, dni);

        // 2. Validar reglas específicas del paciente (actualmente ninguna)
        validateRoleSpecificRules();

        // 3. Crear y guardar el User base
        User savedUser = createAndSaveUser(
                email,
                password,
                Role.ROLE_PATIENT,
                firstName,
                lastName,
                dni,
                phone,
                birthDate
        );

        // 4. Crear y guardar el perfil específico de Patient
        return (Patient) createAndSaveRoleProfile(savedUser);
    }

    @Override
    protected void validateRoleSpecificRules() {
        // Los pacientes no tienen validaciones adicionales por ahora
        // Ejemplo futuro: validar que no tenga deudas pendientes, etc.
    }

    @Override
    protected Patient createAndSaveRoleProfile(User savedUser) {
        // Crear el perfil de Patient con los datos específicos
        Patient patient = new Patient(savedUser, healthInsurance, bloodType);

        // Guardar y retornar el Patient
        return patientRepository.save(patient);
    }
}
