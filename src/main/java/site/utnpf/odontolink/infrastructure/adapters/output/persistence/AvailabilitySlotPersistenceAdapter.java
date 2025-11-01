package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.domain.repository.AvailabilitySlotRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AvailabilitySlotEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaAvailabilitySlotRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.AvailabilitySlotPersistenceMapper;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para AvailabilitySlot (Hexagonal Architecture).
 * Implementa la interfaz del dominio AvailabilitySlotRepository usando JPA.
 * Puerto de salida (Output Adapter).
 *
 * Este adaptador valida que los horarios solicitados estén dentro de la
 * disponibilidad publicada por los practicantes.
 */
@Component
public class AvailabilitySlotPersistenceAdapter implements AvailabilitySlotRepository {

    private final JpaAvailabilitySlotRepository jpaAvailabilitySlotRepository;

    public AvailabilitySlotPersistenceAdapter(JpaAvailabilitySlotRepository jpaAvailabilitySlotRepository) {
        this.jpaAvailabilitySlotRepository = jpaAvailabilitySlotRepository;
    }

    @Override
    public AvailabilitySlot save(AvailabilitySlot availabilitySlot) {
        AvailabilitySlotEntity entity = AvailabilitySlotPersistenceMapper.toEntity(availabilitySlot);
        AvailabilitySlotEntity savedEntity = jpaAvailabilitySlotRepository.save(entity);
        return AvailabilitySlotPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<AvailabilitySlot> findById(Long id) {
        return jpaAvailabilitySlotRepository.findById(id)
                .map(AvailabilitySlotPersistenceMapper::toDomain);
    }

    @Override
    public List<AvailabilitySlot> findByOfferedTreatmentId(Long offeredTreatmentId) {
        return jpaAvailabilitySlotRepository.findByOfferedTreatment_Id(offeredTreatmentId).stream()
                .map(AvailabilitySlotPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Verifica si un horario específico cae dentro de alguna franja de disponibilidad.
     * Esta es una validación crítica en el flujo de reserva de turnos (Regla de Negocio 1).
     */
    @Override
    public boolean isTimeWithinAvailability(Long offeredTreatmentId, DayOfWeek dayOfWeek, LocalTime time) {
        return jpaAvailabilitySlotRepository.isTimeWithinAvailability(offeredTreatmentId, dayOfWeek, time);
    }
}
