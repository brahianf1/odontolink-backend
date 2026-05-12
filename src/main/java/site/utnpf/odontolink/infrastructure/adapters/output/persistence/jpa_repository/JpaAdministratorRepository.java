package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AdministratorEntity;

/**
 * Repositorio JPA de Spring Data para AdministratorEntity.
 * Esta interfaz NO pertenece al dominio, es parte de la infraestructura.
 * Sólo expone las operaciones mínimas que necesita el bootstrap del primer
 * administrador del sistema; el resto de la gestión de administradores se
 * resolverá cuando exista un caso de uso real que lo requiera.
 */
@Repository
public interface JpaAdministratorRepository extends JpaRepository<AdministratorEntity, Long> {

    /**
     * Indica si ya existe un AdministratorEntity asociado al UserEntity dado.
     * Usado por el bootstrap para evitar duplicar el perfil de administrador
     * cuando el usuario base ya fue creado previamente.
     */
    boolean existsByUser_Id(Long userId);
}
