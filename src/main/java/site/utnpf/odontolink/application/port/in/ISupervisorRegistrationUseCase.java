package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Supervisor;

import java.time.LocalDate;

/**
 * Puerto de entrada (Use Case) para el registro de supervisores/docentes.
 * Define el contrato que deben cumplir los servicios de aplicación.
 */
public interface ISupervisorRegistrationUseCase {

    /**
     * Registra un nuevo supervisor/docente en el sistema.
     */
    Supervisor registerSupervisor(String email, String password, String firstName, String lastName,
                                  String dni, String phone, LocalDate birthDate,
                                  String specialty, String employeeId);
}
