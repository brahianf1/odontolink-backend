package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.OfferedTreatmentStatus;

import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * Filtros disponibles para la vista "Mi Catálogo Personal" del practicante.
 *
 * Cada valor mapea a un predicado sobre la oferta. La partición busca ser
 * mutuamente excluyente y exhaustiva sobre el universo de ofertas del
 * practicante (con {@link #ALL} como complemento que no aplica filtro).
 *
 * EXPIRED es un estado derivado: una oferta {@code status=ACTIVE} cuyo
 * {@code offerEndDate} ya pasó. No se persiste como estado del enum
 * {@link OfferedTreatmentStatus} para no requerir un job de transición
 * automática y mantener el modelo de datos minimalista; el cálculo se hace
 * al momento de leer.
 */
public enum OfferedTreatmentListFilter {

    /** Vigente y dentro de la ventana temporal: lo bookable real. */
    ACTIVE {
        @Override
        public Predicate<OfferedTreatment> asPredicate(LocalDate today) {
            return offer -> offer.getStatus() == OfferedTreatmentStatus.ACTIVE
                    && (offer.getOfferEndDate() == null
                        || !offer.getOfferEndDate().isBefore(today));
        }
    },

    /** Pausada voluntariamente por el practicante. */
    PAUSED {
        @Override
        public Predicate<OfferedTreatment> asPredicate(LocalDate today) {
            return offer -> offer.getStatus() == OfferedTreatmentStatus.PAUSED;
        }
    },

    /** Dada de baja lógicamente (RF16). */
    INACTIVE {
        @Override
        public Predicate<OfferedTreatment> asPredicate(LocalDate today) {
            return offer -> offer.getStatus() == OfferedTreatmentStatus.INACTIVE;
        }
    },

    /**
     * ACTIVE pero con {@code offerEndDate} vencida. Bucket para que el
     * practicante identifique las ofertas que necesitan renovación de
     * vigencia o eliminación.
     */
    EXPIRED {
        @Override
        public Predicate<OfferedTreatment> asPredicate(LocalDate today) {
            return offer -> offer.getStatus() == OfferedTreatmentStatus.ACTIVE
                    && offer.getOfferEndDate() != null
                    && offer.getOfferEndDate().isBefore(today);
        }
    },

    /** Sin filtro: devuelve todas las ofertas del practicante. */
    ALL {
        @Override
        public Predicate<OfferedTreatment> asPredicate(LocalDate today) {
            return offer -> true;
        }
    };

    /**
     * Predicado a aplicar sobre cada {@link OfferedTreatment} para decidir
     * inclusión en el resultado.
     *
     * @param today Fecha de referencia para los predicados temporales
     *              (EXPIRED y ACTIVE). Se inyecta para testabilidad y para
     *              que el llamador controle el huso horario.
     */
    public abstract Predicate<OfferedTreatment> asPredicate(LocalDate today);
}
