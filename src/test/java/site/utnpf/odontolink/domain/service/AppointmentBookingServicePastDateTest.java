package site.utnpf.odontolink.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.repository.AppointmentRepository;
import site.utnpf.odontolink.domain.repository.AttentionRepository;
import site.utnpf.odontolink.domain.repository.AvailabilitySlotRepository;
import site.utnpf.odontolink.domain.repository.ChatSessionRepository;
import site.utnpf.odontolink.domain.repository.InstitutionalSettingsRepository;
import site.utnpf.odontolink.domain.repository.NonWorkingDayRepository;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Cubre la defensa de dominio que rechaza reservas con fecha anterior al
 * instante actual. Es complemento del {@code @FutureOrPresent} del DTO; el
 * test garantiza que callers no-HTTP (jobs, tests) no puedan saltarse la
 * regla.
 *
 * <p>Foco: el guard debe disparar ANTES de tocar repositorios, así que
 * verificamos que ningún mock recibió interacciones cuando la fecha está en
 * el pasado.
 */
class AppointmentBookingServicePastDateTest {

    @Test
    @DisplayName("reserva con appointmentTime en el pasado lanza InvalidBusinessRuleException")
    void rejectsPastAppointment() {
        OfferedTreatmentRepository offeredRepo = mock(OfferedTreatmentRepository.class);
        AvailabilitySlotRepository slotRepo = mock(AvailabilitySlotRepository.class);
        AppointmentRepository appointmentRepo = mock(AppointmentRepository.class);
        AttentionRepository attentionRepo = mock(AttentionRepository.class);
        ChatSessionRepository chatRepo = mock(ChatSessionRepository.class);
        InstitutionalSettingsRepository settingsRepo = mock(InstitutionalSettingsRepository.class);
        NonWorkingDayRepository nwdRepo = mock(NonWorkingDayRepository.class);

        AppointmentBookingService service = new AppointmentBookingService(
                offeredRepo, slotRepo, appointmentRepo, attentionRepo, chatRepo, settingsRepo, nwdRepo
        );

        Patient patient = mock(Patient.class);
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        InvalidBusinessRuleException ex = assertThrows(InvalidBusinessRuleException.class,
                () -> service.bookAppointment(patient, 99L, yesterday));

        assertTrue(ex.getMessage().toLowerCase().contains("pasad") ||
                   ex.getMessage().toLowerCase().contains("anterior"));

        // El guard debe abortar antes de consultar la oferta, slots, etc.
        verifyNoInteractions(offeredRepo, slotRepo, appointmentRepo,
                attentionRepo, chatRepo, settingsRepo, nwdRepo);
    }
}
