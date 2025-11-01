package site.utnpf.odontolink.domain.service;

import site.utnpf.odontolink.domain.exception.DomainException;
import site.utnpf.odontolink.domain.exception.DuplicateResourceException;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
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
     * 2. Regla de Validación de Slots: Todos los slots de disponibilidad deben ser válidos
     * 
     * @param practitioner El practicante que ofrece el tratamiento
     * @param treatment El tratamiento maestro seleccionado
     * @param requirements Los requisitos específicos del practicante
     * @param availabilitySlots Los horarios de disponibilidad
     * @return El OfferedTreatment creado (sin persistir aún)
     * @throws DuplicateResourceException si el practicante ya ofrece este tratamiento
     * @throws DomainException si alguna validación falla
     */
    public OfferedTreatment createOffer(Practitioner practitioner, 
                                        Treatment treatment, 
                                        String requirements,
                                        Set<AvailabilitySlot> availabilitySlots) {
        
        // Regla 1: Verificar unicidad (el practicante no puede ofrecer el mismo tratamiento dos veces)
        validateUniqueness(practitioner, treatment);

        // Regla 2: Validar que todos los slots de disponibilidad sean válidos
        validateAvailabilitySlots(availabilitySlots);

        // Crear la oferta
        OfferedTreatment offeredTreatment = new OfferedTreatment(practitioner, treatment, requirements);

        // Agregar los slots de disponibilidad (estableciendo la relación bidireccional)
        if (availabilitySlots != null) {
            for (AvailabilitySlot slot : availabilitySlots) {
                offeredTreatment.addAvailabilitySlot(slot);
            }
        }

        return offeredTreatment;
    }

    /**
     * Actualiza una oferta de tratamiento existente aplicando las reglas de negocio.
     * 
     * @param existingOffer La oferta existente a actualizar
     * @param newRequirements Los nuevos requisitos
     * @param newAvailabilitySlots Los nuevos slots de disponibilidad
     * @return La oferta actualizada
     * @throws DomainException si alguna validación falla
     */
    public OfferedTreatment updateOffer(OfferedTreatment existingOffer,
                                        String newRequirements,
                                        Set<AvailabilitySlot> newAvailabilitySlots) {
        
        // Validar los nuevos slots de disponibilidad
        validateAvailabilitySlots(newAvailabilitySlots);

        // Actualizar requisitos
        existingOffer.setRequirements(newRequirements);

        // Limpiar slots antiguos
        if (existingOffer.getAvailabilitySlots() != null) {
            existingOffer.getAvailabilitySlots().clear();
        }

        // Agregar los nuevos slots
        if (newAvailabilitySlots != null) {
            for (AvailabilitySlot slot : newAvailabilitySlots) {
                existingOffer.addAvailabilitySlot(slot);
            }
        }

        return existingOffer;
    }

    /**
     * Valida que un tratamiento se pueda eliminar del catálogo.
     * 
     * Regla: No se puede eliminar un tratamiento con turnos activos/pendientes.
     * 
     * @param offeredTreatmentId El ID de la oferta a eliminar
     * @throws InvalidBusinessRuleException si la oferta tiene turnos activos
     */
    public void validateCanDelete(Long offeredTreatmentId) {
        if (offeredTreatmentRepository.hasActiveAppointments(offeredTreatmentId)) {
            throw new InvalidBusinessRuleException(
                "No se puede eliminar el tratamiento porque tiene turnos activos o pendientes. " +
                "Debe cancelar o completar todas las atenciones antes de eliminarlo."
            );
        }
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
