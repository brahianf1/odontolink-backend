package site.utnpf.odontolink.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica el invariante de edad mínima del agregado {@link User}. Es la
 * defensa de dominio que complementa el constraint {@code @MinimumAge} de
 * los DTOs: cualquier camino que cree o mute un User salteando la capa REST
 * sigue cubierto por este test.
 */
class UserAgeInvariantTest {

    @Test
    @DisplayName("constructor con birthDate de menor lanza InvalidBusinessRuleException")
    void constructorRejectsMinor() {
        LocalDate birthOfMinor = LocalDate.now().minusYears(17);
        assertThrows(InvalidBusinessRuleException.class, () -> new User(
                "test@example.com",
                "hashed",
                Role.ROLE_PATIENT,
                "Juan",
                "Pérez",
                "12345678",
                null,
                birthOfMinor
        ));
    }

    @Test
    @DisplayName("constructor con birthDate null pasa: el campo es opcional")
    void constructorAllowsNullBirthDate() {
        assertDoesNotThrow(() -> new User(
                "test@example.com",
                "hashed",
                Role.ROLE_PATIENT,
                "Juan",
                "Pérez",
                "12345678",
                null,
                null
        ));
    }

    @Test
    @DisplayName("constructor con exactamente 18 años cumplidos pasa")
    void constructorAcceptsExactlyEighteen() {
        LocalDate eighteenToday = LocalDate.now().minusYears(18);
        assertDoesNotThrow(() -> new User(
                "test@example.com",
                "hashed",
                Role.ROLE_PRACTITIONER,
                "Ana",
                "García",
                "87654321",
                null,
                eighteenToday
        ));
    }

    @Test
    @DisplayName("updateProfile con birthDate de menor revierte la mutación con excepción")
    void updateProfileRejectsMinor() {
        User user = adultUser();
        LocalDate originalBirth = user.getBirthDate();

        assertThrows(InvalidBusinessRuleException.class,
                () -> user.updateProfile("Nuevo", "Nombre", "999", LocalDate.now().minusYears(10)));

        // El estado del agregado no debe quedar parcialmente mutado: la
        // excepción se dispara antes de tocar birthDate, y firstName/lastName
        // se aplican antes en el método. Verificamos que birthDate sigue
        // siendo el original.
        assertEquals(originalBirth, user.getBirthDate());
    }

    @Test
    @DisplayName("changeBirthDate acepta null para limpiar el dato")
    void changeBirthDateAcceptsNull() {
        User user = adultUser();
        user.changeBirthDate(null);
        assertNull(user.getBirthDate());
    }

    @Test
    @DisplayName("changeBirthDate rechaza menor")
    void changeBirthDateRejectsMinor() {
        User user = adultUser();
        assertThrows(InvalidBusinessRuleException.class,
                () -> user.changeBirthDate(LocalDate.now().minusYears(5)));
    }

    @Test
    @DisplayName("setter directo NO valida — está reservado al mapper de persistencia")
    void setterBypassValidationOnPurpose() {
        // Documenta la decisión: el setter Bean-style es la vía de
        // hidratación desde la base. La aplicación debe usar changeBirthDate
        // para activar el invariante. Si alguien cambia este comportamiento
        // sin actualizar el mapper, este test rompe y obliga a discutirlo.
        User user = new User();
        assertDoesNotThrow(() -> user.setBirthDate(LocalDate.now().minusYears(5)));
        assertEquals(LocalDate.now().minusYears(5), user.getBirthDate());
    }

    private User adultUser() {
        return new User(
                "adult@example.com",
                "hashed",
                Role.ROLE_PATIENT,
                "Adulta",
                "Persona",
                "11223344",
                null,
                LocalDate.now().minusYears(25)
        );
    }
}
