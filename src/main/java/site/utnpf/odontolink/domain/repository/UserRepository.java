package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.User;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia de usuarios (Hexagonal Architecture).
 * Esta interfaz pertenece al dominio y será implementada en la capa de infraestructura.
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByDni(String dni);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
}
