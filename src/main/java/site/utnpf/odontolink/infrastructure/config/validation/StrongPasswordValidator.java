package site.utnpf.odontolink.infrastructure.config.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import site.utnpf.odontolink.infrastructure.config.security.CommonPasswordBlocklist;

/**
 * Validador para {@link StrongPassword}. Spring auto-cablea el
 * {@link CommonPasswordBlocklist} gracias a la integración estándar de
 * Spring Boot con Hibernate Validator (SpringConstraintValidatorFactory).
 *
 * <p>Mensajes específicos por violación para que el frontend pueda mostrar al
 * usuario qué exactamente está fallando. Si el campo viene null o vacío
 * dejamos pasar para no solapar responsabilidades con {@code @NotBlank}:
 * cada constraint informa su propia falla y la combinación produce un
 * reporte completo sin redundancia.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private final CommonPasswordBlocklist blocklist;

    private int minLength;
    private int maxLength;
    private boolean checkCommon;

    public StrongPasswordValidator(CommonPasswordBlocklist blocklist) {
        this.blocklist = blocklist;
    }

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
        this.checkCommon = constraintAnnotation.checkCommon();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            // Delegamos a @NotBlank el reporte de "campo obligatorio".
            return true;
        }

        if (value.length() < minLength) {
            return reject(context,
                    "La contraseña debe tener al menos " + minLength + " caracteres.");
        }

        if (value.length() > maxLength) {
            return reject(context,
                    "La contraseña no puede superar los " + maxLength + " caracteres.");
        }

        if (checkCommon && blocklist.contains(value)) {
            return reject(context,
                    "La contraseña es demasiado común. Elija una más difícil de adivinar.");
        }

        return true;
    }

    private boolean reject(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
