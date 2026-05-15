package site.utnpf.odontolink.infrastructure.config.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Carga al arranque la lista de contraseñas comunes prohibidas y expone un
 * test {@link #contains(String)} en O(1) sobre un {@link HashSet}.
 *
 * La lista se lee desde {@code classpath:security/common-passwords.txt}. La
 * carga es defensiva: si el archivo no está disponible la aplicación arranca
 * con un blocklist vacío y emite un WARN — preferimos degradar a "sin
 * blocklist" antes que impedir el arranque del servicio.
 *
 * <p>La normalización a {@code lowercase} ocurre al cargar y al consultar:
 * comparar contraseñas en lower-case es seguro porque {@code BCryptPasswordEncoder}
 * trata mayúsculas y minúsculas como entradas distintas y la mayoría de los
 * usuarios eligen variantes triviales como "Password1" que sólo se diferencian
 * por capitalización.
 */
@Component
public class CommonPasswordBlocklist {

    private static final Logger log = LoggerFactory.getLogger(CommonPasswordBlocklist.class);
    private static final String CLASSPATH_LOCATION = "security/common-passwords.txt";

    private final Set<String> blockedPasswords = new HashSet<>();

    @PostConstruct
    void load() {
        ClassPathResource resource = new ClassPathResource(CLASSPATH_LOCATION);
        if (!resource.exists()) {
            log.warn("Common-password blocklist not found at classpath:{}; password policy will run without it.",
                    CLASSPATH_LOCATION);
            return;
        }

        try (InputStream in = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                blockedPasswords.add(trimmed.toLowerCase());
            }

            log.info("Loaded {} entries into the common-password blocklist.", blockedPasswords.size());
        } catch (IOException ex) {
            log.warn("Failed to read common-password blocklist; continuing with empty blocklist.", ex);
        }
    }

    /**
     * @return {@code true} si la contraseña proporcionada coincide (case
     *         insensitive) con alguna entrada de la blocklist.
     */
    public boolean contains(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return false;
        }
        return blockedPasswords.contains(rawPassword.toLowerCase());
    }

    /**
     * Tamaño actual de la blocklist. Útil para diagnóstico/health endpoints.
     */
    public int size() {
        return blockedPasswords.size();
    }
}
