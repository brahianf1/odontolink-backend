package site.utnpf.odontolink.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import site.utnpf.odontolink.infrastructure.config.security.CustomUserDetailsService;
import site.utnpf.odontolink.infrastructure.config.security.JwtAuthenticationFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de Spring Security.
 * Define las políticas de seguridad, autenticación y autorización.
 * Incluye configuración dinámica de CORS desde variables de entorno.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                         JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/patients/register").permitAll()
                        .requestMatchers("/api/practitioners/register").permitAll()
                        .requestMatchers("/api/supervisors/register").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // RF05 y RF07: todo lo que cuelgue de /api/admin/** queda
                        // reservado al rol ROLE_ADMIN. Esta regla declarativa se
                        // suma a los @PreAuthorize de los controllers como
                        // defensa en profundidad: si alguien retira la anotación
                        // por accidente, la cadena de filtros sigue bloqueando.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(daoAuthenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configuración de CORS basada en variables de entorno.
     * Permite configurar diferentes orígenes según el ambiente (dev, prod).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Convertir string de orígenes separados por coma a lista
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Permitir credenciales
        configuration.setAllowCredentials(true);

        // Tiempo de cache de preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * DaoAuthenticationProvider instanciado manualmente y NO expuesto como bean.
     *
     * Si lo expusieramos como @Bean, Spring Security detecta un AuthenticationProvider
     * global y desactiva la auto-configuracion del UserDetailsService sobre el
     * AuthenticationManager (warning HHH/InitializeUserDetailsManagerConfigurer).
     * Manteniendo la instancia local: el AuthenticationManager global se cablea de
     * forma transparente con el CustomUserDetailsService + PasswordEncoder presentes
     * en el contexto, y esta cadena de filtros usa explicitamente el provider para
     * el flujo de username/password (login).
     *
     * Se usa el constructor introducido en Spring Security 6.5 que recibe el
     * UserDetailsService: el setter equivalente quedo deprecado.
     */
    private DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
