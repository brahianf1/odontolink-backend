package site.utnpf.odontolink.infrastructure.config.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.Period;

/**
 * Validador para {@link MinimumAge}. Calcula la edad real cumplida a la
 * fecha del sistema usando {@link Period} para que funcione correctamente
 * tanto en años bisiestos como en el día exacto del cumpleaños.
 *
 * <p>Decisiones de diseño:
 * <ul>
 *   <li>Si el valor es {@code null} devuelve {@code true}: la obligatoriedad
 *       la cubren otros constraints (típicamente {@code @NotNull}) y los
 *       campos opcionales no deben dispararse si vienen omitidos.</li>
 *   <li>Si el valor está en el futuro devuelve {@code true}: ese caso lo
 *       reporta {@code @Past}, evitando mensajes redundantes.</li>
 *   <li>Mensaje personalizado con la edad mínima requerida para que el
 *       frontend muestre un texto accionable al usuario.</li>
 * </ul>
 */
public class MinimumAgeValidator implements ConstraintValidator<MinimumAge, LocalDate> {

    private int minimumAgeYears;

    @Override
    public void initialize(MinimumAge constraintAnnotation) {
        this.minimumAgeYears = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate today = LocalDate.now();
        if (value.isAfter(today)) {
            // Fecha futura: lo reporta @Past, no este constraint.
            return true;
        }
        int years = Period.between(value, today).getYears();
        if (years >= minimumAgeYears) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "Debes tener al menos " + minimumAgeYears + " años para registrarte."
        ).addConstraintViolation();
        return false;
    }
}
