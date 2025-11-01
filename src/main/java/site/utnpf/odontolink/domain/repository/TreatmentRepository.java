package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Treatment;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para el catálogo maestro de tratamientos.
 * Puerto de salida (Output Port) en arquitectura hexagonal.
 */
public interface TreatmentRepository {

    /**
     * Guarda un nuevo tratamiento o actualiza uno existente.
     */
    Treatment save(Treatment treatment);

    /**
     * Busca un tratamiento por su ID.
     */
    Optional<Treatment> findById(Long id);

    /**
     * Obtiene todos los tratamientos del catálogo maestro.
     */
    List<Treatment> findAll();

    /**
     * Verifica si existe un tratamiento con el nombre dado.
     */
    boolean existsByName(String name);

    /**
     * Elimina un tratamiento por su ID.
     */
    void deleteById(Long id);
}
