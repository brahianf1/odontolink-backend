package site.utnpf.odontolink.domain.model;

/**
 * Estado del ciclo de vida de una {@link OfferedTreatment} dentro del catálogo
 * personal del practicante.
 *
 * Reemplaza al booleano {@code active} previo: el booleano representaba dos
 * conceptos distintos (vigente vs. baja lógica de RF16) y no permitía modelar
 * la pausa voluntaria temporal del practicante. El enum cierra ese gap y hace
 * imposible representar combinaciones inválidas (p.ej. un "pausado e inactivo"
 * a la vez).
 *
 * Reglas de visibilidad y bookability:
 * <ul>
 *   <li>{@link #ACTIVE}: única forma "vigente" — aparece en el catálogo público
 *       y acepta nuevas reservas.</li>
 *   <li>{@link #PAUSED}: pausa voluntaria del practicante. Se oculta del
 *       catálogo público y rechaza nuevas reservas, pero preserva la oferta
 *       como un recurso del practicante listo para ser reanudado. Los turnos
 *       y atenciones ya existentes no se ven afectados.</li>
 *   <li>{@link #INACTIVE}: baja lógica de RF16. Se aplica cuando el
 *       practicante elimina la oferta y existen compromisos vivos o
 *       atenciones históricas que impiden el borrado físico. Se oculta del
 *       catálogo público y rechaza nuevas reservas. Reactivable solo
 *       explícitamente vía endpoint dedicado.</li>
 * </ul>
 *
 * Reglas de unicidad:
 * El catálogo personal no admite dos ofertas para el mismo treatment en
 * estado {@link #ACTIVE} o {@link #PAUSED} simultáneamente (ambos ocupan el
 * "slot" del practicante para ese tratamiento). Una oferta {@link #INACTIVE}
 * no compite por la unicidad: el practicante puede crear una nueva para el
 * mismo tratamiento.
 */
public enum OfferedTreatmentStatus {
    /** Vigente, visible en el catálogo público y aceptando reservas. */
    ACTIVE,

    /** Pausa voluntaria temporal del practicante. Oculta y no bookable. */
    PAUSED,

    /** Baja lógica (RF16). Oculta, no bookable, reactivable explícitamente. */
    INACTIVE;

    /** Verdadero solo para {@link #ACTIVE}: única condición de bookability. */
    public boolean isBookable() {
        return this == ACTIVE;
    }

    /** Verdadero para los estados que ocupan el slot de unicidad del par practitioner+treatment. */
    public boolean occupiesUniquenessSlot() {
        return this == ACTIVE || this == PAUSED;
    }
}
