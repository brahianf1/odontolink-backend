package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.UserEntity;

import java.util.Optional;

/**
 * Repositorio JPA de Spring Data para UserEntity.
 * Esta interfaz NO pertenece al dominio, es parte de la infraestructura.
 */
@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByDni(String dni);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
}
