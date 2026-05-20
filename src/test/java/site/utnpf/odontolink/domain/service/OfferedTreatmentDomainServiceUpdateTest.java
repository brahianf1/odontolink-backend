package site.utnpf.odontolink.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Cubre las reglas de fecha en el update de oferta (Regla 2 del PR):
 *   - una oferta vigente puede editarse manteniendo su startDate aunque sea
 *     pasada, sin romper por ello;
 *   - no se permite "retroceder" el startDate a una fecha pasada distinta
 *     de la original (back-dating).
 *
 * Crear ofertas con startDate pasada es imposible vía el constructor del
 * POJO ({@code OfferedTreatment} valida no-pasado al crear), así que para
 * simular una oferta legítimamente vigente con startDate ayer, usamos
 * setters después de construir.
 */
class OfferedTreatmentDomainServiceUpdateTest {

    private OfferedTreatmentDomainService domainService;

    @BeforeEach
    void setUp() {
        OfferedTreatmentRepository repo = mock(OfferedTreatmentRepository.class);
        domainService = new OfferedTreatmentDomainService(repo);
    }

    @Test
    @DisplayName("update conserva startDate pasada original sin lanzar")
    void updateKeepsHistoricalStartDate() {
        OfferedTreatment offer = legitOfferWithBackdatedStartDate();
        LocalDate previousStart = offer.getOfferStartDate();

        assertDoesNotThrow(() -> domainService.updateOffer(
                offer,
                "nuevos requirements",
                60,
                Set.of(slot()),
                previousStart,             // misma startDate ya pasada
                LocalDate.now().plusMonths(2),
                10
        ));
        assertEquals("nuevos requirements", offer.getRequirements());
    }

    @Test
    @DisplayName("update rechaza mover startDate al pasado distinto de la original")
    void updateRejectsBackdating() {
        OfferedTreatment offer = legitOfferWithBackdatedStartDate();
        LocalDate movedFurtherIntoPast = offer.getOfferStartDate().minusDays(5);

        InvalidBusinessRuleException ex = assertThrows(InvalidBusinessRuleException.class,
                () -> domainService.updateOffer(
                        offer,
                        "x",
                        60,
                        Set.of(slot()),
                        movedFurtherIntoPast,
                        LocalDate.now().plusMonths(1),
                        10
                ));
        assert ex.getMessage().toLowerCase().contains("pasado");
    }

    @Test
    @DisplayName("update permite adelantar startDate a futuro")
    void updateAcceptsForwardStartDate() {
        OfferedTreatment offer = legitOfferWithBackdatedStartDate();
        LocalDate forward = LocalDate.now().plusDays(7);

        assertDoesNotThrow(() -> domainService.updateOffer(
                offer,
                "x",
                60,
                Set.of(slot()),
                forward,
                LocalDate.now().plusMonths(2),
                10
        ));
        assertEquals(forward, offer.getOfferStartDate());
    }

    /**
     * Construye una oferta que en su momento se creó válida (startDate ==
     * today al crear) y, por el paso del tiempo, hoy tiene una startDate
     * pasada. El constructor del POJO no permite recrear ese escenario
     * directamente con una fecha pasada, así que la simulamos con un
     * {@code setOfferStartDate} posterior. Refleja exactamente lo que pasa
     * en producción cuando una oferta lleva varios días vigente.
     */
    private OfferedTreatment legitOfferWithBackdatedStartDate() {
        Practitioner practitioner = mock(Practitioner.class);
        Treatment treatment = mock(Treatment.class);
        OfferedTreatment offer = new OfferedTreatment(
                practitioner,
                treatment,
                Set.of(slot()),
                60,
                LocalDate.now(),
                LocalDate.now().plusMonths(1),
                5
        );
        // Avanzamos el reloj relativo: la oferta empezó hace 3 días.
        offer.setOfferStartDate(LocalDate.now().minusDays(3));
        return offer;
    }

    private AvailabilitySlot slot() {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setDayOfWeek(DayOfWeek.MONDAY);
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(12, 0));
        return slot;
    }
}
