package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;

import java.time.LocalDate;
import java.util.List;

/**
 * Puerto de entrada para la gestión de usuarios por parte del administrador (RF05).
 *
 * Cubre las cuatro operaciones del requisito: listar, crear, modificar y dar
 * de baja lógica. La creación se delega a los casos de uso de registro
 * existentes ({@code IPatientRegistrationUseCase}, {@code IPractitionerRegistrationUseCase},
 * {@code ISupervisorRegistrationUseCase}) para mantener DRY y reutilizar las
 * validaciones de unicidad y la creación de los perfiles específicos por rol.
 *
 * La reactivación se incluye como complemento natural de la baja lógica:
 * sin ella, un administrador que se equivoque al desactivar a un usuario
 * quedaría obligado a recrear el registro, perdiendo el historial.
 */
public interface IAdminUserManagementUseCase {

    /**
     * Listado filtrado de usuarios para el panel de administración.
     *
     * @param role     filtro por rol (opcional)
     * @param isActive filtro por estado (opcional). {@code null} devuelve activos e inactivos.
     * @param query    búsqueda libre por email, DNI o nombre completo (opcional)
     */
    List<User> listUsers(Role role, Boolean isActive, String query);

    /**
     * Obtiene el detalle de un usuario por su ID.
     *
     * @throws site.utnpf.odontolink.domain.exception.ResourceNotFoundException si no existe
     */
    User getUserById(Long id);

    /**
     * Crea un paciente. Reusa la validación de unicidad de email/DNI ya
     * implementada en el caso de uso de registro de pacientes.
     */
    Patient createPatient(String email, String password, String firstName, String lastName,
                          String dni, String phone, LocalDate birthDate,
                          String healthInsurance, String bloodType);

    /**
     * Crea un practicante. Reusa la validación de unicidad de email/DNI y
     * legajo del caso de uso de registro de practicantes.
     */
    Practitioner createPractitioner(String email, String password, String firstName, String lastName,
                                    String dni, String phone, LocalDate birthDate,
                                    String studentId, Integer studyYear);

    /**
     * Crea un supervisor/docente. Reusa la validación de unicidad de
     * email/DNI y legajo docente del caso de uso de registro de supervisores.
     */
    Supervisor createSupervisor(String email, String password, String firstName, String lastName,
                                String dni, String phone, LocalDate birthDate,
                                String specialty, String employeeId);

    /**
     * Actualiza los datos de perfil de un usuario. Sólo se permiten los
     * campos seguros (nombre, apellido, teléfono, fecha de nacimiento): el
     * email, el DNI, el rol y la contraseña tienen flujos separados o
     * implicaciones de seguridad que se manejan fuera de esta operación.
     */
    User updateUserProfile(Long id, String firstName, String lastName,
                           String phone, LocalDate birthDate);

    /**
     * Baja lógica: marca al usuario como inactivo manteniendo el registro
     * histórico. El usuario inactivo no podrá iniciar sesión gracias al
     * chequeo de {@code isActive} en {@code CustomUserDetailsService}.
     */
    User deactivateUser(Long id);

    /**
     * Revierte una baja lógica previa. Operación complementaria de
     * {@link #deactivateUser(Long)} y restringida a administradores.
     */
    User reactivateUser(Long id);
}
