package site.utnpf.odontolink.domain.model;

import java.time.DayOfWeek;

/**
 * Value Object inmutable que encapsula los criterios de búsqueda dinámica
 * del catálogo público de tratamientos ofrecidos (RF09).
 *
 * Pertenece al Dominio porque expresa una intención de negocio (qué buscar)
 * y no debe acoplar la capa de aplicación a Spring/JPA. La traducción a
 * predicados SQL ocurre en el adaptador de persistencia mediante
 * Specifications.
 *
 * Todos los campos son OPCIONALES y combinables (AND lógico):
 * - keyword: subcadena buscada en nombre/descripción del tratamiento
 *   y en nombre/apellido del practicante (case-insensitive).
 * - specialty: área odontológica exacta del tratamiento (ej. "ORTODONCIA").
 * - availabilityDay: día de la semana sobre el que la oferta publica al
 *   menos un AvailabilitySlot.
 *
 * Se exponen helpers booleanos {@code has*} para que el adaptador
 * construya predicados sólo cuando el campo viene informado, evitando
 * cláusulas WHERE inútiles que confundirían al optimizador del motor.
 */
public final class OfferedTreatmentSearchCriteria {

    private final String keyword;
    private final String specialty;
    private final DayOfWeek availabilityDay;

    public OfferedTreatmentSearchCriteria(String keyword,
                                          String specialty,
                                          DayOfWeek availabilityDay) {
        // Normalizamos a null las cadenas en blanco para unificar el "no filtra"
        // en una única representación y simplificar las ramas de los helpers.
        this.keyword = isBlank(keyword) ? null : keyword.trim();
        this.specialty = isBlank(specialty) ? null : specialty.trim();
        this.availabilityDay = availabilityDay;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getSpecialty() {
        return specialty;
    }

    public DayOfWeek getAvailabilityDay() {
        return availabilityDay;
    }

    public boolean hasKeyword() {
        return keyword != null;
    }

    public boolean hasSpecialty() {
        return specialty != null;
    }

    public boolean hasAvailabilityDay() {
        return availabilityDay != null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
