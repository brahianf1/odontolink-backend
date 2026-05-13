package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.User;

/**
 * Puerto de entrada (Use Case) para el autoservicio de perfil del usuario (RF06).
 *
 * Modela las tres operaciones del flujo:
 * <ol>
 *   <li>Lectura del propio perfil para que el frontend pueda prellenar
 *       formularios sin tener que consultar endpoints de otros bounded
 *       contexts.</li>
 *   <li>Actualización de información personal y de contacto (correo,
 *       teléfono, dirección, foto de perfil, nombre/apellido, fecha de
 *       nacimiento).</li>
 *   <li>Cambio de contraseña autenticado (con verificación de la
 *       contraseña actual), independiente del flujo de recuperación público
 *       de RF04.</li>
 * </ol>
 *
 * Decisión arquitectónica importante: las operaciones reciben el
 * {@code userId} del usuario autenticado como parámetro. La extracción de
 * la identidad desde el JWT vive en la capa de infraestructura
 * (AuthenticationFacade), no aquí. Así mantenemos la capa de aplicación
 * agnóstica al mecanismo de seguridad — testeable con un Long simple y
 * blindada contra ataques de IDOR porque ningún endpoint expone el ID en
 * la URL.
 */
public interface IProfileUseCase {

    /**
     * Devuelve el perfil completo del usuario autenticado.
     *
     * @param userId identificador del usuario obtenido del contexto de seguridad
     * @throws site.utnpf.odontolink.domain.exception.ResourceNotFoundException
     *         si el usuario no existe (situación que sólo podría darse si la
     *         cuenta fue eliminada físicamente con sesión activa).
     */
    User getMyProfile(Long userId);

    /**
     * Actualiza los datos modificables por el propio usuario (RF06).
     *
     * El email se incluye porque la tesis lo enumera explícitamente; el
     * servicio valida unicidad con {@code existsByEmailAndIdNot} para no
     * marcar al propio usuario como duplicado. El DNI y el rol permanecen
     * inmutables: son identificadores funcionales y reescribirlos desde
     * autoservicio rompería la trazabilidad clínica y administrativa.
     *
     * @return el usuario actualizado tal como quedó persistido
     * @throws site.utnpf.odontolink.domain.exception.DuplicateResourceException
     *         si el nuevo email ya pertenece a otra cuenta.
     */
    User updateMyProfile(Long userId, UpdateProfileCommand command);

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * Es un endpoint distinto del flujo de recuperación pública (RF04):
     * aquí el usuario ya está autenticado y debe demostrar conocimiento de
     * la contraseña actual antes de poder rotarla. Esta verificación es
     * obligatoria para mitigar ataques de "session-fixation + password
     * change" si el atacante obtuviera temporalmente la sesión.
     *
     * @throws site.utnpf.odontolink.domain.exception.AuthenticationFailedException
     *         si la contraseña actual no coincide con la almacenada.
     */
    void changeMyPassword(Long userId, String currentPassword, String newPassword);
}
