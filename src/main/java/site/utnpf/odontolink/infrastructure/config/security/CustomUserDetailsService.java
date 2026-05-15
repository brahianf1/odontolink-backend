package site.utnpf.odontolink.infrastructure.config.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.UserRepository;

import java.util.Collection;
import java.util.Collections;

/**
 * Implementación personalizada de UserDetailsService para Spring Security.
 * Carga los datos del usuario desde nuestro repositorio de dominio y devuelve
 * un {@link OdontolinkUserDetails} que lleva los campos extra que necesitamos
 * en el filtro JWT (userId, passwordChangedAt).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + email);
        }

        return new OdontolinkUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.isActive(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                getAuthorities(user),
                user.getPasswordChangedAt()
        );
    }

    /**
     * Convierte el Role del dominio a GrantedAuthority de Spring Security.
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()));
    }
}
