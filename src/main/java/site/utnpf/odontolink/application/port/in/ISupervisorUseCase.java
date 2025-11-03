package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.User;

import java.util.List;
import java.util.Set;

/**
 * Puerto de entrada para los casos de uso de gestión de supervisión académica.
 * Define las operaciones disponibles para supervisores (docentes) en el contexto académico.
 *
 * Este puerto sigue el principio de segregación de interfaces (ISP) y define
 * los contratos de los siguientes casos de uso:
 * - CU 7.1: Vincular Practicante (RF22, RF37)
 * - CU 7.2: Desvincular Practicante
 * - CU 7.3: Visualizar y Buscar Practicantes (RF35, RF38)
 *
 * Filosofía de Diseño:
 * - Desacopla el contexto académico (Supervisor-Practicante) del contexto clínico
 * - El supervisor es el actor activo que inicia las vinculaciones
 * - Proporciona funcionalidades de "observador" sobre el trabajo de los practicantes
 *
 * @author OdontoLink Team
 */
public interface ISupervisorUseCase {

    /**
     * Vincula un practicante a la lista de alumnos supervisados del supervisor.
     * Implementa CU 7.1 - RF22, RF37.
     *
     * Este método establece una relación bidireccional N-a-N entre el supervisor
     * y el practicante, habilitando al supervisor para:
     * - Ver feedback de las atenciones del practicante
     * - Supervisar el progreso académico del alumno
     * - Acceder a las atenciones registradas por el practicante
     *
     * @param practitionerId ID del practicante a vincular
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @throws site.utnpf.odontolink.domain.exception.ResourceNotFoundException si no existe el practicante o el supervisor
     * @throws site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException si el vínculo ya existe
     */
    void linkPractitionerToSupervisor(Long practitionerId, User supervisorUser);

    /**
     * Desvincula un practicante de la lista de alumnos supervisados del supervisor.
     * Implementa CU 7.2.
     *
     * Este método rompe la relación de supervisión académica. Después de desvincular,
     * el supervisor pierde acceso a:
     * - Feedback de las atenciones del practicante
     * - Información académica detallada del alumno
     * - Historial de atenciones
     *
     * @param practitionerId ID del practicante a desvincular
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @throws site.utnpf.odontolink.domain.exception.ResourceNotFoundException si no existe el practicante o el supervisor
     * @throws site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException si no existe el vínculo
     */
    void unlinkPractitionerFromSupervisor(Long practitionerId, User supervisorUser);

    /**
     * Obtiene el conjunto de practicantes que el supervisor tiene a su cargo.
     * Implementa CU 7.3 (Endpoint A) - RF35.
     *
     * Retorna todos los practicantes con vínculo activo de supervisión.
     * Esta información es fundamental para la UI del supervisor.
     *
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @return Conjunto de practicantes supervisados (puede estar vacío)
     * @throws site.utnpf.odontolink.domain.exception.ResourceNotFoundException si no existe el supervisor
     */
    Set<Practitioner> getMyPractitioners(User supervisorUser);

    /**
     * Vincula múltiples practicantes a la lista de alumnos supervisados del supervisor.
     * Operación batch que permite vincular varios practicantes en una sola transacción.
     *
     * Este método es útil para:
     * - Setup inicial de supervisión (asignar múltiples alumnos de una vez)
     * - Operaciones masivas de vinculación
     * - Mejorar la experiencia de usuario en el frontend
     *
     * Comportamiento:
     * - Vincula todos los practicantes que no estén ya vinculados (idempotente)
     * - Ignora (skip) los que ya estén vinculados
     * - Operación atómica: todos se vinculan o ninguno
     *
     * @param practitionerIds Lista de IDs de practicantes a vincular
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @throws site.utnpf.odontolink.domain.exception.ResourceNotFoundException si algún practicante o el supervisor no existe
     * @throws IllegalArgumentException si la lista está vacía o es nula
     */
    void linkMultiplePractitioners(List<Long> practitionerIds, User supervisorUser);

    /**
     * Busca practicantes en el sistema mediante criterios de búsqueda.
     * Implementa CU 7.3 (Endpoint B) - RF38.
     *
     * Permite al supervisor buscar practicantes por:
     * - Nombre completo (búsqueda parcial, case-insensitive)
     * - DNI (documento)
     * - Legajo (student ID)
     *
     * Si no se proporciona query (null), retorna todos los practicantes activos.
     * Esta funcionalidad es necesaria para que el supervisor pueda:
     * - Explorar el listado completo de practicantes disponibles
     * - Buscar practicantes específicos para vincular
     *
     * @param query Término de búsqueda opcional (nombre, DNI o legajo). Si es null, retorna todos
     * @return Lista de practicantes que coinciden con la búsqueda o todos si query es null
     */
    List<Practitioner> searchPractitioners(String query);
}
