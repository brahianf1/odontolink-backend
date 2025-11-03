package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.Practitioner;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para la persistencia de practicantes (Hexagonal Architecture).
 * Esta interfaz pertenece al dominio y será implementada en la capa de infraestructura.
 */
public interface PractitionerRepository {
    Practitioner save(Practitioner practitioner);
    Optional<Practitioner> findById(Long id);
    Optional<Practitioner> findByUserId(Long userId);
    boolean existsByStudentId(String studentId);

    /**
     * Busca practicantes por múltiples criterios: nombre, DNI o legajo.
     * Implementa búsqueda flexible para el supervisor.
     *
     * @param query Término de búsqueda (parcial, case-insensitive)
     * @return Lista de practicantes que coinciden con el criterio
     */
    List<Practitioner> searchByQuery(String query);

    /**
     * Obtiene todos los practicantes del sistema.
     * Útil para listados completos cuando no se requiere filtrado.
     *
     * @return Lista de todos los practicantes
     */
    List<Practitioner> findAll();
}
