package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.UserRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.UserEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaUserRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.UserPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para User (Hexagonal Architecture).
 * Implementa la interfaz del dominio UserRepository usando JPA.
 * Puerto de salida (Output Adapter).
 */
@Component
public class UserPersistenceAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    public UserPersistenceAdapter(JpaUserRepository jpaUserRepository) {
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserPersistenceMapper.toEntity(user);
        UserEntity savedEntity = jpaUserRepository.save(entity);
        return UserPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id)
                .map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByDni(String dni) {
        return jpaUserRepository.findByDni(dni)
                .map(UserPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByDni(String dni) {
        return jpaUserRepository.existsByDni(dni);
    }

    @Override
    public List<User> findAllByFilters(Role role, Boolean isActive, String query) {
        return jpaUserRepository.findAllByFilters(role, isActive, query).stream()
                .map(UserPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long excludingId) {
        return jpaUserRepository.existsByEmailAndIdNot(email, excludingId);
    }

    @Override
    public boolean existsByDniAndIdNot(String dni, Long excludingId) {
        return jpaUserRepository.existsByDniAndIdNot(dni, excludingId);
    }
}
