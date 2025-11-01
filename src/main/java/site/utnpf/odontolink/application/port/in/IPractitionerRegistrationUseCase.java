package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Practitioner;

import java.time.LocalDate;

/**
 * Puerto de entrada (Use Case) para el registro de practicantes.
 * Define el contrato que deben cumplir los servicios de aplicación.
 */
public interface IPractitionerRegistrationUseCase {

    /**
     * Registra un nuevo practicante en el sistema.
     */
    Practitioner registerPractitioner(String email, String password, String firstName, String lastName,
                                     String dni, String phone, LocalDate birthDate,
                                     String studentId, Integer studyYear);
}
