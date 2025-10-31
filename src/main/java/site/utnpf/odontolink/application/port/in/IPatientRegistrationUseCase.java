package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Patient;

import java.time.LocalDate;

/**
 * Puerto de entrada (Use Case) para el registro de pacientes.
 * Define el contrato que deben cumplir los servicios de aplicación.
 */
public interface IPatientRegistrationUseCase {

    /**
     * Registra un nuevo paciente en el sistema.
     */
    Patient registerPatient(String email, String password, String firstName, String lastName,
                           String dni, String phone, LocalDate birthDate,
                           String healthInsurance, String bloodType);
}
