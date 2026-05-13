package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.DomainException;
import site.utnpf.odontolink.domain.exception.DuplicateResourceException;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.OfferedTreatmentDeletionResult;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.model.Treatment;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;

import java.util.Set;

/**
 * Servicio de Dominio que contiene las reglas de negocio para OfferedTreatment.
 *
 * Responsabilidades:
 * - Validar reglas de negocio relacionadas con las ofertas de tratamiento
 * - Aplicar invariantes del dominio
 * - Coordinar operaciones complejas del dominio
 */
public class OfferedTreatmentDomainService {

    private final OfferedTreatmentRepository offeredTreatmentRepository;

    public OfferedTreatmentDomainService(OfferedTreatmentRepository offeredTreatmentRepository) {
        this.offeredTreatmentRepository = offeredTreatmentRepository;
    }

    /**
     * Crea una nueva oferta de tratamiento aplicando todas las reglas de negocio.
     *
     * Reglas aplicadas:
     * 1. Regla de Unicidad: Un practicante no puede ofrecer el mismo tratamiento dos veces
     * 2. Regla de Validación de Duración: La duración debe ser mayor a 0
     * 3. Regla de Validación de Slots: Todos los slots de disponibilidad deben ser válidos
     *
     * @param practitioner El practicante que ofrece el tratamiento
     * @param treatment El tratamiento maestro seleccionado
     * @param requirements Los requisitos específicos del practicante
     * @param durationInMinutes Duración del tratamiento en minutos
     * @param availabilitySlots Los horarios de disponibilidad
     * @param offerStartDate Fecha de inicio de la oferta
     * @param offerEndDate Fecha de fin de la oferta
     * @param maxCompletedAttentions Cupo máximo de casos completados
     * @return El OfferedTreatment creado (sin persistir aún)
     * @throws DuplicateResourceException si el practicante ya ofrece este tratamiento
     * @throws DomainException si alguna validación falla
     */
    public OfferedTreatment createOffer(Practitioner practitioner,
                                        Treatment treatment,
                                        String requirements,
                                        int durationInMinutes,
                                        Set<AvailabilitySlot> availabilitySlots,
                                        java.time.LocalDate offerStartDate,
                                        java.time.LocalDate offerEndDate,
                                        Integer maxCompletedAttentions) {

        validateUniqueness(practitioner, treatment);
        validateDuration(durationInMinutes);
        validateAvailabilitySlots(availabilitySlots);

        OfferedTreatment offeredTreatment = new OfferedTreatment(
                practitioner,
                treatment,
                availabilitySlots,
                durationInMinutes,
                offerStartDate,
                offerEndDate,
                maxCompletedAttentions
        );

        offeredTreatment.setRequirements(requirements);

        return offeredTreatment;
    }

    /**
     * Actualiza una oferta de tratamiento existente aplicando las reglas de negocio.
     *
     * Todos los campos relacionados con límites son obligatorios en la actualización
     * para garantizar que las ofertas siempre mantengan límites claros definidos.
     *
     * @param existingOffer La oferta existente a actualizar
     * @param newRequirements Los nuevos requisitos
     * @param newDurationInMinutes Nueva duración en minutos (null para no modificar)
     * @param newAvailabilitySlots Los nuevos slots de disponibilidad
     * @param newOfferStartDate Nueva fecha de inicio (obligatoria)
     * @param newOfferEndDate Nueva fecha de fin (obligatoria)
     * @param newMaxCompletedAttentions Nuevo cupo máximo (obligatorio)
     * @return La oferta actualizada
     * @throws DomainException si alguna validación falla
     */
    public OfferedTreatment updateOffer(OfferedTreatment existingOffer,
                                        String newRequirements,
                                        Integer newDurationInMinutes,
                                        Set<AvailabilitySlot> newAvailabilitySlots,
                                        java.time.LocalDate newOfferStartDate,
                                        java.time.LocalDate newOfferEndDate,
                                        Integer newMaxCompletedAttentions) {

        validateRequiredUpdateFields(newOfferStartDate, newOfferEndDate, newMaxCompletedAttentions);

        if (newDurationInMinutes != null) {
            validateDuration(newDurationInMinutes);
            existingOffer.setDurationInMinutes(newDurationInMinutes);
        }

        validateAvailabilitySlots(newAvailabilitySlots);

        existingOffer.setRequirements(newRequirements);

        if (existingOffer.getAvailabilitySlots() != null) {
            existingOffer.getAvailabilitySlots().clear();
        }

        if (newAvailabilitySlots != null) {
            for (AvailabilitySlot slot : newAvailabilitySlots) {
                existingOffer.addAvailabilitySlot(slot);
            }
        }

        existingOffer.setOfferStartDate(newOfferStartDate);
        existingOffer.setOfferEndDate(newOfferEndDate);
        existingOffer.setMaxCompletedAttentions(newMaxCompletedAttentions);

        return existingOffer;
    }

    /**
     * Valida que los campos obligatorios de la actualización no sean nulos.
     */
    private void validateRequiredUpdateFields(java.time.LocalDate startDate, java.time.LocalDate endDate, Integer maxAttentions) {
        if (startDate == null) {
            throw new InvalidBusinessRuleException("La fecha de inicio de la oferta es obligatoria.");
        }
        if (endDate == null) {
            throw new InvalidBusinessRuleException("La fecha de fin de la oferta es obligatoria.");
        }
        if (maxAttentions == null) {
            throw new InvalidBusinessRuleException("El cupo máximo de casos completados es obligatorio.");
        }
    }

    /**
     * Decide la estrategia de eliminación de una oferta del catálogo (RF16).
     *
     * Política del Dominio:
     * 1. Si existen turnos SCHEDULED a futuro asociados al par practitioner+treatment
     *    de la oferta, hay compromisos vivos con pacientes: NO se puede borrar físicamente,
     *    se aplica Baja Lógica (soft delete) para retirarla del catálogo público
     *    preservando la cadena Appointment → Attention.
     * 2. Si existen Atenciones IN_PROGRESS para el par, hay casos clínicos abiertos
     *    que aún pueden recibir nuevos turnos/evolutivos: también Baja Lógica.
     * 3. Si existen Atenciones históricas (COMPLETED / CANCELLED) pero ningún
     *    compromiso vivo, igualmente preferimos Baja Lógica para no perder la
     *    cadena de navegación del historial clínico desde Attention.treatment.
     *    El dato histórico siempre se conserva, alineado con el espíritu de RF16.
     * 4. Sólo si NO existe ningún rastro histórico (oferta nunca usada) se permite
     *    el borrado físico (hard delete) — limpieza barata para errores de catálogo.
     *
     * El método NO muta ni persiste: devuelve la decisión y deja al servicio
     * de aplicación coordinar la operación correspondiente y la transacción.
     *
     * @param offer La oferta cargada en memoria sobre la que se está decidiendo
     * @return El outcome del Dominio: SOFT_DELETED o HARD_DELETED, con su razón
     */
    public OfferedTreatmentDeletionResult resolveDeletionStrategy(OfferedTreatment offer) {
        Long offerId = offer.getId();

        if (offeredTreatmentRepository.hasFutureScheduledAppointments(offerId)) {
            return new OfferedTreatmentDeletionResult(
                    OfferedTreatmentDeletionResult.Outcome.SOFT_DELETED,
                    "La oferta se desactivó: existen turnos agendados a futuro. " +
                    "Se conserva la integridad referencial de las citas ya otorgadas. " +
                    "Ya no aparecerá en el catálogo público."
            );
        }

        if (offeredTreatmentRepository.hasInProgressAttentions(offerId)) {
            return new OfferedTreatmentDeletionResult(
                    OfferedTreatmentDeletionResult.Outcome.SOFT_DELETED,
                    "La oferta se desactivó: existen casos clínicos en curso para este tratamiento. " +
                    "Se conserva la integridad de los expedientes activos. " +
                    "Ya no aparecerá en el catálogo público."
            );
        }

        if (offeredTreatmentRepository.hasHistoricalAttentions(offerId)) {
            return new OfferedTreatmentDeletionResult(
                    OfferedTreatmentDeletionResult.Outcome.SOFT_DELETED,
                    "La oferta se desactivó: existen atenciones históricas asociadas. " +
                    "Se conserva el historial clínico navegable. " +
                    "Ya no aparecerá en el catálogo público."
            );
        }

        return new OfferedTreatmentDeletionResult(
                OfferedTreatmentDeletionResult.Outcome.HARD_DELETED,
                "La oferta fue eliminada del catálogo. No existían turnos ni atenciones asociadas."
        );
    }

    /**
     * Valida la regla de unicidad: un practicante no puede ofrecer el mismo tratamiento dos veces.
     */
    private void validateUniqueness(Practitioner practitioner, Treatment treatment) {
        if (offeredTreatmentRepository.existsByPractitionerAndTreatment(practitioner, treatment)) {
            throw new InvalidBusinessRuleException(
                "El tratamiento '" + treatment.getName() + "' ya existe en su catálogo personal. " +
                "Si desea modificarlo, utilice la opción de editar."
            );
        }
    }

    /**
     * Valida la duración del tratamiento.
     *
     * Regla: La duración debe ser mayor a 0 y razonable (no más de 480 minutos - 8 horas).
     */
    private void validateDuration(int durationInMinutes) {
        if (durationInMinutes <= 0) {
            throw new InvalidBusinessRuleException(
                "La duración del tratamiento debe ser mayor a 0 minutos."
            );
        }
        if (durationInMinutes > 480) {
            throw new InvalidBusinessRuleException(
                "La duración del tratamiento no puede ser mayor a 480 minutos (8 horas)."
            );
        }
        // Validar que la duración sea múltiplo de 5 para facilitar el agendamiento
        if (durationInMinutes % 5 != 0) {
            throw new InvalidBusinessRuleException(
                "La duración del tratamiento debe ser un múltiplo de 5 minutos."
            );
        }
    }

    /**
     * Valida que todos los slots de disponibilidad sean válidos.
     *
     * Regla: startTime debe ser anterior a endTime.
     */
    private void validateAvailabilitySlots(Set<AvailabilitySlot> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new InvalidBusinessRuleException("Debe especificar al menos un horario de disponibilidad.");
        }

        for (AvailabilitySlot slot : slots) {
            if (!slot.isValid()) {
                throw new InvalidBusinessRuleException(
                    "Horario inválido: la hora de inicio debe ser anterior a la hora de fin."
                );
            }
        }
    }
}
