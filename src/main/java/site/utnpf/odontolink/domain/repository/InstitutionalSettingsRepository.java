package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.InstitutionalSettings;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia de la configuración institucional (RF07).
 *
 * El agregado se trata como un singleton: existe a lo sumo una fila en la
 * base de datos. Sólo se exponen las dos operaciones que el caso de uso
 * realmente necesita (leer y persistir), evitando ampliar la superficie
 * del puerto con métodos que no tienen sentido en un agregado único.
 */
public interface InstitutionalSettingsRepository {

    /**
     * Devuelve la configuración institucional vigente, si existe.
     */
    Optional<InstitutionalSettings> findSingleton();

    /**
     * Persiste el agregado. La implementación es responsable de garantizar
     * la unicidad de la fila aprovechando el {@code id} fijo del singleton.
     */
    InstitutionalSettings save(InstitutionalSettings settings);
}
