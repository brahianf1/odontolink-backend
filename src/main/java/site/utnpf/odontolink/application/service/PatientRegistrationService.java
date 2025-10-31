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
 * Coordina la lógica de negocio para crear un usuario con rol PATIENT y su perfil asociado.
 * El bean se registra explícitamente en BeanConfiguration.
 */
@Transactional
public class PatientRegistrationService implements IPatientRegistrationUseCase {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientRegistrationService(UserRepository userRepository,
                                     PatientRepository patientRepository,
                                     PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Patient registerPatient(String email, String password, String firstName, String lastName,
                                   String dni, String phone, LocalDate birthDate,
                                   String healthInsurance, String bloodType) {

        // Validación de negocio: El email no debe existir
        if (userRepository.existsByEmail(email)) {
            throw new site.utnpf.odontolink.domain.exception.DuplicateResourceException("Usuario", "email", email);
        }

        // Validación de negocio: El DNI no debe existir
        if (userRepository.existsByDni(dni)) {
            throw new site.utnpf.odontolink.domain.exception.DuplicateResourceException("Usuario", "DNI", dni);
        }

        // Crear el User con contraseña hasheada
        User user = new User(
                email,
                passwordEncoder.encode(password),
                Role.ROLE_PATIENT,
                firstName,
                lastName,
                dni,
                phone,
                birthDate
        );

        // Guardar el User primero
        User savedUser = userRepository.save(user);

        // Crear el perfil de Patient
        Patient patient = new Patient(savedUser, healthInsurance, bloodType);

        // Guardar el Patient
        return patientRepository.save(patient);
    }
}
