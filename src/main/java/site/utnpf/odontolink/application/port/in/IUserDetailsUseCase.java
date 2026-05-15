package site.utnpf.odontolink.application.port.in;

import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Puerto de entrada para la lectura y actualizacion de datos rol-especificos
 * del usuario autenticado.
 *
 * <p>Separado del {@link IProfileUseCase} para mantener cada caso de uso
 * con su superficie minima:
 * <ul>
 *   <li>{@code IProfileUseCase} gestiona datos comunes (en {@code User}).</li>
 *   <li>{@code IUserDetailsUseCase} gestiona campos almacenados en las tablas
 *       hija ({@code patients}, {@code practitioners}, {@code supervisors})
 *       con reglas de "que es editable" distintas por rol.</li>
 * </ul>
 *
 * <p>Decisiones de campos editables (acordadas con FE el 2026-05-15):
 * <ul>
 *   <li>PATIENT: {@code healthInsurance}, {@code bloodType} editables.</li>
 *   <li>PRACTITIONER: TODO read-only ({@code studentId}, {@code studyYear}
 *       son identidad academica asignada por la facultad).</li>
 *   <li>SUPERVISOR: {@code specialty} editable; {@code employeeId} read-only
 *       (legajo docente).</li>
 *   <li>ADMIN: sin campos extra.</li>
 * </ul>
 */
public interface IUserDetailsUseCase {

    /**
     * Devuelve la vista de los datos rol-especificos del usuario. Los
     * campos no aplicables al rol del usuario vuelven en {@code null}.
     */
    MyDetailsView getMyDetails(Long userId);

    /**
     * PATCH de los datos clinicos del PATIENT. Lanza
     * {@link site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException}
     * si el usuario no es PATIENT.
     */
    void updatePatientDetails(Long userId,
                              JsonNullable<String> healthInsurance,
                              JsonNullable<String> bloodType);

    /**
     * PATCH de los datos academicos del SUPERVISOR. Lanza
     * {@link site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException}
     * si el usuario no es SUPERVISOR.
     */
    void updateSupervisorDetails(Long userId, JsonNullable<String> specialty);
}
