package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.ISupervisorUseCase;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Supervisor;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.SupervisorRepository;
import site.utnpf.odontolink.domain.service.SupervisorPolicyService;

import java.util.List;
import java.util.Set;

/**
 * Servicio de aplicación para la gestión de supervisión académica.
 * Implementa el puerto de entrada ISupervisorUseCase siguiendo la Arquitectura Hexagonal.
 *
 * Este servicio es el orquestador transaccional de los casos de uso:
 * - CU 7.1: Vincular Practicante (RF22, RF37)
 * - CU 7.2: Desvincular Practicante
 * - CU 7.3: Visualizar y Buscar Practicantes (RF35, RF38)
 *
 * Su responsabilidad principal es coordinar:
 * 1. La carga de entidades de dominio desde los repositorios
 * 2. La delegación de lógica de negocio al servicio de dominio (SupervisorPolicyService)
 * 3. La persistencia transaccional de los cambios
 * 4. El manejo de errores y excepciones de dominio
 *
 * Flujo de ejecución típico:
 * Controller -> SupervisorService (aquí) -> SupervisorPolicyService (dominio) -> Repositories
 *
 * Filosofía de Diseño:
 * - Desacopla el contexto académico del contexto clínico
 * - El supervisor actúa como "observador" que requiere vinculación previa
 * - Toda modificación de relaciones N-a-N debe ser bidireccional y transaccional
 *
 * @Transactional asegura que toda la operación sea atómica.
 *
 * @author OdontoLink Team
 */
@Transactional
public class SupervisorService implements ISupervisorUseCase {

    private final SupervisorRepository supervisorRepository;
    private final PractitionerRepository practitionerRepository;
    private final SupervisorPolicyService supervisorPolicyService;

    public SupervisorService(
            SupervisorRepository supervisorRepository,
            PractitionerRepository practitionerRepository,
            SupervisorPolicyService supervisorPolicyService) {
        this.supervisorRepository = supervisorRepository;
        this.practitionerRepository = practitionerRepository;
        this.supervisorPolicyService = supervisorPolicyService;
    }

    /**
     * Implementa el caso de uso CU 7.1: Vincular Practicante.
     *
     * Orquestación:
     * 1. Busca el Supervisor asociado al usuario autenticado
     * 2. Busca el Practitioner por ID
     * 3. Delega al servicio de dominio para establecer el vínculo bidireccional
     * 4. Persiste los cambios de forma transaccional
     *
     * @param practitionerId ID del practicante a vincular
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @throws ResourceNotFoundException si no existe el practicante o el supervisor
     * @throws site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException si el vínculo ya existe
     */
    @Override
    public void linkPractitionerToSupervisor(Long practitionerId, User supervisorUser) {
        // Cargar el Supervisor desde el repositorio
        Supervisor supervisor = supervisorRepository.findByUserId(supervisorUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Supervisor", "userId", supervisorUser.getId().toString()));

        // Cargar el Practitioner desde el repositorio
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Practitioner", "id", practitionerId.toString()));

        // Delegar al servicio de dominio para aplicar las reglas de negocio
        // El servicio de dominio valida:
        // - Que no exista un vínculo duplicado
        // - Establece la relación bidireccional correctamente
        supervisorPolicyService.linkPractitioner(supervisor, practitioner);

        // Persistir ambas entidades para mantener la consistencia bidireccional
        // JPA manejará la tabla de unión en base a la configuración @ManyToMany
        supervisorRepository.save(supervisor);
        practitionerRepository.save(practitioner);
    }

    /**
     * Implementa el caso de uso CU 7.2: Desvincular Practicante.
     *
     * Orquestación:
     * 1. Busca el Supervisor asociado al usuario autenticado
     * 2. Busca el Practitioner por ID
     * 3. Delega al servicio de dominio para romper el vínculo bidireccional
     * 4. Persiste los cambios de forma transaccional
     *
     * @param practitionerId ID del practicante a desvincular
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @throws ResourceNotFoundException si no existe el practicante o el supervisor
     * @throws site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException si no existe el vínculo
     */
    @Override
    public void unlinkPractitionerFromSupervisor(Long practitionerId, User supervisorUser) {
        // Cargar el Supervisor desde el repositorio
        Supervisor supervisor = supervisorRepository.findByUserId(supervisorUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Supervisor", "userId", supervisorUser.getId().toString()));

        // Cargar el Practitioner desde el repositorio
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Practitioner", "id", practitionerId.toString()));

        // Delegar al servicio de dominio para aplicar las reglas de negocio
        // El servicio de dominio valida:
        // - Que exista el vínculo previamente
        // - Rompe la relación bidireccional correctamente
        supervisorPolicyService.unlinkPractitioner(supervisor, practitioner);

        // Persistir ambas entidades para mantener la consistencia bidireccional
        supervisorRepository.save(supervisor);
        practitionerRepository.save(practitioner);
    }

    /**
     * Implementa el caso de uso CU 7.3 (Endpoint A): Obtener mis practicantes.
     *
     * Orquestación:
     * 1. Busca el Supervisor asociado al usuario autenticado
     * 2. Retorna el conjunto de practicantes supervisados
     *
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @return Conjunto de practicantes supervisados (puede estar vacío)
     * @throws ResourceNotFoundException si no existe el supervisor
     */
    @Override
    @Transactional(readOnly = true)
    public Set<Practitioner> getMyPractitioners(User supervisorUser) {
        // Cargar el Supervisor desde el repositorio
        Supervisor supervisor = supervisorRepository.findByUserId(supervisorUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Supervisor", "userId", supervisorUser.getId().toString()));

        // Retornar el conjunto de practicantes supervisados
        // El conjunto puede estar vacío si el supervisor no tiene practicantes asignados
        Set<Practitioner> practitioners = supervisor.getSupervisedPractitioners();
        return practitioners != null ? practitioners : Set.of();
    }

    /**
     * Implementa la vinculación múltiple de practicantes (operación batch).
     *
     * Orquestación:
     * 1. Valida que la lista no esté vacía
     * 2. Busca el Supervisor asociado al usuario autenticado
     * 3. Itera sobre cada ID de practicante
     * 4. Delega al servicio de dominio para establecer cada vínculo
     * 5. Persiste todos los cambios en una única transacción
     *
     * Esta operación es atómica: o se vinculan todos los practicantes o ninguno.
     * Los vínculos duplicados son ignorados (operación idempotente).
     *
     * @param practitionerIds Lista de IDs de practicantes a vincular
     * @param supervisorUser Usuario autenticado con rol SUPERVISOR
     * @throws IllegalArgumentException si la lista es nula o vacía
     * @throws ResourceNotFoundException si algún practicante o el supervisor no existe
     */
    @Override
    public void linkMultiplePractitioners(List<Long> practitionerIds, User supervisorUser) {
        // Validación de entrada
        if (practitionerIds == null || practitionerIds.isEmpty()) {
            throw new IllegalArgumentException("La lista de IDs de practicantes no puede estar vacía.");
        }

        // Cargar el Supervisor desde el repositorio
        Supervisor supervisor = supervisorRepository.findByUserId(supervisorUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Supervisor", "userId", supervisorUser.getId().toString()));

        // Iterar sobre cada ID y vincular (operación batch)
        for (Long practitionerId : practitionerIds) {
            // Cargar el Practitioner desde el repositorio
            Practitioner practitioner = practitionerRepository.findById(practitionerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Practitioner", "id", practitionerId.toString()));

            // Delegar al servicio de dominio para establecer el vínculo
            // Si el vínculo ya existe, el servicio de dominio lo maneja (puede lanzar excepción o ignorar)
            // Aquí decidimos hacerlo idempotente: intentamos vincular, si ya existe, continuamos
            try {
                supervisorPolicyService.linkPractitioner(supervisor, practitioner);
            } catch (site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException e) {
                // Si el error es por vínculo duplicado, lo ignoramos (idempotencia)
                // Otros errores se propagan
                if (!e.getMessage().contains("ya está vinculado")) {
                    throw e;
                }
                // Si ya está vinculado, continuamos con el siguiente
            }
        }

        // Persistir los cambios de forma transaccional
        // JPA manejará la tabla de unión automáticamente
        supervisorRepository.save(supervisor);
    }

    /**
     * Implementa el caso de uso CU 7.3 (Endpoint B): Buscar practicantes globalmente.
     *
     * Orquestación:
     * 1. Si query es null o vacío, retorna todos los practicantes
     * 2. Si query tiene valor, delega la búsqueda al repositorio con el criterio especificado
     * 3. Retorna la lista de practicantes (sin filtrar por supervisor)
     *
     * Esta funcionalidad permite al supervisor:
     * - Explorar todos los practicantes disponibles en el sistema
     * - Buscar practicantes específicos por nombre, DNI o legajo
     * - Encontrar practicantes para vincular a su cargo
     *
     * @param query Término de búsqueda opcional (nombre, DNI o legajo). Si es null, retorna todos
     * @return Lista de practicantes que coinciden con la búsqueda o todos si query es null
     */
    @Override
    @Transactional(readOnly = true)
    public List<Practitioner> searchPractitioners(String query) {
        // Si no hay query o está vacío, retornar todos los practicantes
        if (query == null || query.trim().isEmpty()) {
            return practitionerRepository.findAll();
        }

        // Delegar la búsqueda al repositorio
        // El repositorio implementa la lógica de búsqueda multi-campo
        return practitionerRepository.searchByQuery(query.trim());
    }
}
