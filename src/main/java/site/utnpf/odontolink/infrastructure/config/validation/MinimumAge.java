package site.utnpf.odontolink.infrastructure.config.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Constraint de Bean Validation que exige una edad mínima sobre un
 * {@link java.time.LocalDate} (típicamente {@code birthDate}).
 *
 * <p>Regla de producto: Odontolink no permite cuentas de menores. La edad
 * se calcula como {@code Period.between(value, today).getYears()} y se
 * compara con {@link #value()} (por defecto 18). El valor nulo se considera
 * válido por diseño: la obligatoriedad la cubre {@code @NotNull} cuando
 * aplique, y los campos opcionales pueden venir omitidos sin que el
 * constraint los rechace.
 *
 * <p>Convivencia con {@code @Past}: ambos son complementarios. {@code @Past}
 * niega fechas futuras (un nacimiento en 2030 es inválido por motivo propio
 * y no por edad); {@code @MinimumAge} niega fechas demasiado recientes para
 * cumplir la edad mínima. Quien se encuentre con un valor fuera de rango
 * recibe el mensaje específico, sin solaparse.
 */
@Documented
@Constraint(validatedBy = MinimumAgeValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface MinimumAge {

    String message() default "La edad no alcanza el mínimo requerido.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Edad mínima requerida en años cumplidos. Por defecto 18 (mayoría de
     * edad civil en Argentina, donde opera Odontolink).
     */
    int value() default 18;
}
