package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.Treatment;

import java.util.List;

/**
 * Puerto de entrada (Input Port) para casos de uso relacionados con el catálogo maestro de tratamientos.
 * Define las operaciones que la aplicación expone para gestionar tratamientos.
 */
public interface ITreatmentUseCase {

    /**
     * Crea un nuevo tratamiento en el catálogo maestro.
     * Solo accesible por ADMIN.
     *
     * @param name Nombre del tratamiento
     * @param description Descripción del tratamiento
     * @param area Área odontológica (ej: "General", "Ortodoncia")
     * @return El tratamiento creado
     */
    Treatment createTreatment(String name, String description, String area);

    /**
     * Obtiene todos los tratamientos del catálogo maestro.
     * Accesible por todos los usuarios autenticados.
     *
     * @return Lista de todos los tratamientos disponibles
     */
    List<Treatment> getAllTreatments();

    /**
     * Busca un tratamiento por su ID.
     *
     * @param id ID del tratamiento
     * @return El tratamiento encontrado
     */
    Treatment getTreatmentById(Long id);
}
