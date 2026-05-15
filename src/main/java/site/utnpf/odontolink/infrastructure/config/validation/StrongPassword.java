package site.utnpf.odontolink.infrastructure.config.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Constraint de Bean Validation que aplica la política de contraseñas de
 * Odontolink (alineada con NIST SP 800-63B):
 * <ul>
 *   <li>Longitud entre {@code minLength} (default 8) y {@code maxLength}
 *       (default 128). BCrypt trunca a 72 bytes; mantenemos el techo holgado
 *       sin acercarnos a ese límite para no producir desencriptados
 *       silenciosamente truncados.</li>
 *   <li>No puede coincidir con la lista de contraseñas comunes prohibidas
 *       (ver {@link site.utnpf.odontolink.infrastructure.config.security.CommonPasswordBlocklist}).</li>
 * </ul>
 *
 * <p>Sigue las recomendaciones del NIST 2026:
 * <ul>
 *   <li>NO se exige composición de mayúsculas + dígitos + símbolos: esa
 *       política empuja a passwords débiles como "Password1!".</li>
 *   <li>SE PRIORIZA longitud + filtrado contra listas de filtraciones
 *       conocidas, que es lo que realmente baja la entropía de ataque.</li>
 * </ul>
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "La contraseña no cumple con la política de seguridad.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int minLength() default 8;

    int maxLength() default 128;

    /**
     * Cuando es {@code true}, la contraseña se confronta contra la blocklist
     * de contraseñas comunes. Se mantiene el flag por si en pruebas de
     * integración se requiere desactivarlo puntualmente, pero el default es
     * siempre validar.
     */
    boolean checkCommon() default true;
}
