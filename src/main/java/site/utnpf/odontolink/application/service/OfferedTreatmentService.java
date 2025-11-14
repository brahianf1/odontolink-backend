package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IOfferedTreatmentUseCase;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.exception.UnauthorizedOperationException;
import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;
import site.utnpf.odontolink.domain.repository.PractitionerRepository;
import site.utnpf.odontolink.domain.repository.TreatmentRepository;
import site.utnpf.odontolink.domain.service.OfferedTreatmentDomainService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Servicio de aplicación para la gestión del catálogo personal de tratamientos del practicante.
 * Implementa el puerto de entrada IOfferedTreatmentUseCase siguiendo la Arquitectura Hexagonal.
 *
 * Este servicio actúa como orquestador entre:
 * - Los repositorios (obtención y persistencia de entidades)
 * - El servicio de dominio (aplicación de reglas de negocio complejas)
 * - Los casos de uso solicitados desde la capa de presentación
 *
 * Responsabilidades:
 * - Orquestar el flujo de casos de uso (CU-005, CU-006, CU-007)
 * - Cargar las entidades necesarias desde los repositorios
 * - Delegar lógica de negocio compleja al servicio de dominio
 * - Verificar permisos de acceso y autorización
 * - Gestionar transacciones de base de datos
 *
 * Casos de uso implementados:
 * - CU-005: Agregar Tratamiento al Catálogo Personal
 * - CU-006: Modificar Tratamiento del Catálogo Personal
 * - CU-007: Eliminar Tratamiento del Catálogo Personal
 *
 * El bean se registra explícitamente en BeanConfiguration.
 *
 * @author OdontoLink Team
 */
@Transactional
public class OfferedTreatmentService implements IOfferedTreatmentUseCase {

    private final OfferedTreatmentRepository offeredTreatmentRepository;
    private final PractitionerRepository practitionerRepository;
    private final TreatmentRepository treatmentRepository;
    private final AttentionRepository attentionRepository;
    private final OfferedTreatmentDomainService domainService;

    public OfferedTreatmentService(OfferedTreatmentRepository offeredTreatmentRepository,
                                   PractitionerRepository practitionerRepository,
                                   TreatmentRepository treatmentRepository,
                                   AttentionRepository attentionRepository,
                                   OfferedTreatmentDomainService domainService) {
        this.offeredTreatmentRepository = offeredTreatmentRepository;
        this.practitionerRepository = practitionerRepository;
        this.treatmentRepository = treatmentRepository;
        this.attentionRepository = attentionRepository;
        this.domainService = domainService;
    }

    /**
     * Agrega un tratamiento al catálogo personal del practicante.
     * Implementa el caso de uso CU-005.
     *
     * Flujo:
     * 1. Carga las entidades Practitioner y Treatment desde los repositorios
     * 2. Delega al servicio de dominio para aplicar las reglas de negocio
     * 3. Persiste la oferta creada junto con sus slots de disponibilidad
     *
     * @param practitionerId ID del practicante que ofrece el tratamiento
     * @param treatmentId ID del tratamiento del catálogo maestro
     * @param requirements Requisitos específicos definidos por el practicante
     * @param availabilitySlots Horarios de disponibilidad (objetos de dominio)
     * @return El tratamiento ofrecido creado y persistido
     * @throws ResourceNotFoundException si el practicante o tratamiento no existen
     */
    @Override
    public OfferedTreatment addTreatmentToCatalog(Long practitionerId,
                                                  Long treatmentId,
                                                  String requirements,
                                                  int durationInMinutes,
                                                  Set<AvailabilitySlot> availabilitySlots,
                                                  java.time.LocalDate offerStartDate,
                                                  java.time.LocalDate offerEndDate,
                                                  Integer maxCompletedAttentions) {

        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner", "id", practitionerId.toString()));

        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", "id", treatmentId.toString()));

        OfferedTreatment offeredTreatment = domainService.createOffer(
                practitioner,
                treatment,
                requirements,
                durationInMinutes,
                availabilitySlots,
                offerStartDate,
                offerEndDate,
                maxCompletedAttentions
        );

        return offeredTreatmentRepository.save(offeredTreatment);
    }

    /**
     * Actualiza un tratamiento del catálogo personal del practicante.
     * Implementa el caso de uso CU-006.
     *
     * Flujo:
     * 1. Carga la oferta existente desde el repositorio
     * 2. Verifica que el practicante autenticado sea el propietario
     * 3. Delega al servicio de dominio para actualizar según las reglas de negocio
     * 4. Persiste los cambios realizados
     *
     * @param practitionerId ID del practicante (para verificación de permisos)
     * @param offeredTreatmentId ID de la oferta a modificar
     * @param requirements Nuevos requisitos
     * @param durationInMinutes Nueva duración (null para no modificar)
     * @param availabilitySlots Nuevos horarios de disponibilidad (objetos de dominio)
     * @param offerStartDate Nueva fecha de inicio (null para no modificar)
     * @param offerEndDate Nueva fecha de fin (null para no modificar)
     * @param maxCompletedAttentions Nuevo cupo máximo (null para no modificar)
     * @return El tratamiento ofrecido actualizado
     * @throws ResourceNotFoundException si la oferta no existe
     * @throws UnauthorizedOperationException si el practicante no es el propietario
     */
    @Override
    public OfferedTreatment updateOfferedTreatment(Long practitionerId,
                                                   Long offeredTreatmentId,
                                                   String requirements,
                                                   Integer durationInMinutes,
                                                   Set<AvailabilitySlot> availabilitySlots,
                                                   java.time.LocalDate offerStartDate,
                                                   java.time.LocalDate offerEndDate,
                                                   Integer maxCompletedAttentions) {

        OfferedTreatment existingOffer = offeredTreatmentRepository.findById(offeredTreatmentId)
                .orElseThrow(() -> new ResourceNotFoundException("OfferedTreatment", "id", offeredTreatmentId.toString()));

        if (!existingOffer.getPractitioner().getId().equals(practitionerId)) {
            throw new UnauthorizedOperationException("No tiene permisos para modificar este tratamiento.");
        }

        OfferedTreatment updatedOffer = domainService.updateOffer(
                existingOffer,
                requirements,
                durationInMinutes,
                availabilitySlots,
                offerStartDate,
                offerEndDate,
                maxCompletedAttentions
        );

        return offeredTreatmentRepository.save(updatedOffer);
    }

    /**
     * Elimina un tratamiento del catálogo personal del practicante.
     * Implementa el caso de uso CU-007.
     *
     * Flujo:
     * 1. Carga la oferta existente desde el repositorio
     * 2. Verifica que el practicante autenticado sea el propietario
     * 3. Valida con el servicio de dominio que no existan turnos activos
     * 4. Elimina la oferta del catálogo
     *
     * @param practitionerId ID del practicante (para verificación de permisos)
     * @param offeredTreatmentId ID de la oferta a eliminar
     * @throws ResourceNotFoundException si la oferta no existe
     * @throws UnauthorizedOperationException si el practicante no es el propietario
     * @throws InvalidBusinessRuleException si existen turnos activos asociados
     */
    @Override
    public void removeFromCatalog(Long practitionerId, Long offeredTreatmentId) {

        OfferedTreatment existingOffer = offeredTreatmentRepository.findById(offeredTreatmentId)
                .orElseThrow(() -> new ResourceNotFoundException("OfferedTreatment", "id", offeredTreatmentId.toString()));

        if (!existingOffer.getPractitioner().getId().equals(practitionerId)) {
            throw new UnauthorizedOperationException("No tiene permisos para eliminar este tratamiento.");
        }

        domainService.validateCanDelete(offeredTreatmentId);
        offeredTreatmentRepository.deleteById(offeredTreatmentId);
    }

    /**
     * Obtiene todos los tratamientos ofrecidos por un practicante específico.
     *
     * @param practitionerId ID del practicante
     * @return Lista de tratamientos ofrecidos
     * @throws ResourceNotFoundException si el practicante no existe
     */
    @Override
    @Transactional(readOnly = true)
    public List<OfferedTreatment> getMyOfferedTreatments(Long practitionerId) {
        if (!practitionerRepository.findById(practitionerId).isPresent()) {
            throw new ResourceNotFoundException("Practitioner", "id", practitionerId.toString());
        }

        return offeredTreatmentRepository.findByPractitionerId(practitionerId);
    }

    /**
     * Busca un tratamiento ofrecido por su ID.
     *
     * @param offeredTreatmentId ID del tratamiento ofrecido
     * @return El tratamiento ofrecido encontrado
     * @throws ResourceNotFoundException si no existe
     */
    @Override
    @Transactional(readOnly = true)
    public OfferedTreatment getOfferedTreatmentById(Long offeredTreatmentId) {
        return offeredTreatmentRepository.findById(offeredTreatmentId)
                .orElseThrow(() -> new ResourceNotFoundException("OfferedTreatment", "id", offeredTreatmentId.toString()));
    }

    /**
     * Obtiene el progreso de atenciones completadas para un practicante,
     * agrupadas por tratamiento.
     *
     * Este método implementa una consulta optimizada que evita el problema N+1:
     * en lugar de ejecutar una consulta por cada oferta del practicante,
     * se ejecuta una única consulta con GROUP BY que retorna todos los conteos.
     *
     * @param practitionerId ID del practicante
     * @return Map donde la clave es el ID del tratamiento y el valor es el conteo de atenciones completadas
     * @throws ResourceNotFoundException si el practicante no existe
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getCompletedAttentionsProgressForPractitioner(Long practitionerId) {
        Practitioner practitioner = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner", "id", practitionerId.toString()));

        return attentionRepository.countCompletedByPractitionerGroupByTreatment(practitioner);
    }
}
