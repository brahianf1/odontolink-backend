package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.UserEntity;

/**
 * Mapper para convertir entre User (dominio) y UserEntity (persistencia).
 * Siguiendo el patrón de Arquitectura Hexagonal.
 */
public class UserPersistenceMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio.
     */
    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        User user = new User();
        user.setId(entity.getId());
        user.setEmail(entity.getEmail());
        user.setPassword(entity.getPassword());
        user.setRole(entity.getRole());
        user.setActive(entity.isActive());
        user.setFirstName(entity.getFirstName());
        user.setLastName(entity.getLastName());
        user.setDni(entity.getDni());
        user.setPhone(entity.getPhone());
        user.setBirthDate(entity.getBirthDate());
        user.setCreatedAt(entity.getCreatedAt());

        return user;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA.
     */
    public static UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setRole(user.getRole());
        entity.setActive(user.isActive());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setDni(user.getDni());
        entity.setPhone(user.getPhone());
        entity.setBirthDate(user.getBirthDate());
        entity.setCreatedAt(user.getCreatedAt());

        return entity;
    }
}
