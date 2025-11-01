package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.ITreatmentUseCase;
import site.utnpf.odontolink.domain.exception.DuplicateResourceException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.domain.repository.TreatmentRepository;

import java.util.List;

/**
 * Servicio de aplicación para la gestión del catálogo maestro de tratamientos.
 * Implementa el puerto de entrada ITreatmentUseCase.
 *
 * Este servicio coordina las operaciones relacionadas con el catálogo maestro
 * que es gestionado por los administradores del sistema.
 *
 * Responsabilidades:
 * - Crear nuevos tratamientos en el catálogo maestro
 * - Consultar tratamientos disponibles
 * - Validar reglas de negocio simples (ej: unicidad de nombres)
 * - Gestionar transacciones
 *
 * El bean se registra explícitamente en BeanConfiguration.
 *
 * @author OdontoLink Team
 */
@Transactional
public class TreatmentService implements ITreatmentUseCase {

    private final TreatmentRepository treatmentRepository;

    public TreatmentService(TreatmentRepository treatmentRepository) {
        this.treatmentRepository = treatmentRepository;
    }

    /**
     * Crea un nuevo tratamiento en el catálogo maestro.
     * Valida que no exista un tratamiento con el mismo nombre.
     *
     * @param name Nombre del tratamiento
     * @param description Descripción detallada
     * @param area Área odontológica a la que pertenece
     * @return El tratamiento creado y persistido
     * @throws DuplicateResourceException si ya existe un tratamiento con ese nombre
     */
    @Override
    public Treatment createTreatment(String name, String description, String area) {
        if (treatmentRepository.existsByName(name)) {
            throw new DuplicateResourceException("Treatment", "name", name);
        }

        Treatment treatment = new Treatment(name, description, area);
        return treatmentRepository.save(treatment);
    }

    /**
     * Obtiene todos los tratamientos del catálogo maestro.
     *
     * @return Lista de todos los tratamientos disponibles
     */
    @Override
    @Transactional(readOnly = true)
    public List<Treatment> getAllTreatments() {
        return treatmentRepository.findAll();
    }

    /**
     * Busca un tratamiento específico por su ID.
     *
     * @param id ID del tratamiento a buscar
     * @return El tratamiento encontrado
     * @throws ResourceNotFoundException si no existe un tratamiento con ese ID
     */
    @Override
    @Transactional(readOnly = true)
    public Treatment getTreatmentById(Long id) {
        return treatmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", "id", id.toString()));
    }
}
