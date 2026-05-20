package site.utnpf.odontolink.infrastructure.config.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests del validador de edad mínima. Cubre:
 *   - tolerancia ante null y fechas futuras (no es su responsabilidad).
 *   - cálculo correcto en el borde exacto del cumpleaños.
 *   - configuración con valores distintos al default de 18.
 *
 * Se usa un stub artesanal de {@link MinimumAge} para parametrizar la edad
 * sin levantar el contexto de Spring ni Hibernate Validator.
 */
class MinimumAgeValidatorTest {

    private MinimumAgeValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new MinimumAgeValidator();
        validator.initialize(minimumAgeOf(18));
        context = mock(ConstraintValidatorContext.class);
        // Hibernate Validator devuelve un builder real; el código de la
        // validación lo encadena pero no inspecciona el resultado, así que
        // un mock no-op es suficiente para que no explote en NPE.
        ConstraintValidatorContext.ConstraintViolationBuilder builder =
                mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(context.buildConstraintViolationWithTemplate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(builder);
    }

    @Test
    @DisplayName("null se considera válido — la obligatoriedad la cubre @NotNull")
    void nullIsValid() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    @DisplayName("fecha futura se considera válida — el rechazo lo hace @Past")
    void futureDateIsValidForThisConstraint() {
        assertTrue(validator.isValid(LocalDate.now().plusDays(1), context));
    }

    @Test
    @DisplayName("30 años cumplidos pasa")
    void thirtyYearsOldIsValid() {
        assertTrue(validator.isValid(LocalDate.now().minusYears(30), context));
    }

    @Test
    @DisplayName("exactamente 18 años cumplidos hoy es válido")
    void exactlyEighteenTodayIsValid() {
        assertTrue(validator.isValid(LocalDate.now().minusYears(18), context));
    }

    @Test
    @DisplayName("17 años 364 días NO alcanza")
    void almostEighteenIsInvalid() {
        LocalDate birth = LocalDate.now().minusYears(18).plusDays(1);
        assertFalse(validator.isValid(birth, context));
    }

    @Test
    @DisplayName("constraint con value=21 rechaza a alguien de 20")
    void customMinimumAgeRespected() {
        validator.initialize(minimumAgeOf(21));
        assertFalse(validator.isValid(LocalDate.now().minusYears(20), context));
        assertTrue(validator.isValid(LocalDate.now().minusYears(21), context));
    }

    /**
     * Crea una instancia de la anotación {@link MinimumAge} con el {@code
     * value} indicado. Evita levantar Hibernate Validator para tests
     * unitarios. {@code annotationType()} es el único método imprescindible
     * para que {@link MinimumAgeValidator#initialize(MinimumAge)} funcione.
     */
    private MinimumAge minimumAgeOf(int minAge) {
        return new MinimumAge() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return MinimumAge.class;
            }

            @Override
            public String message() {
                return "";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() {
                @SuppressWarnings("unchecked")
                Class<? extends jakarta.validation.Payload>[] empty = new Class[0];
                return empty;
            }

            @Override
            public int value() {
                return minAge;
            }
        };
    }
}
