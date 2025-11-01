package site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper;

import site.utnpf.odontolink.domain.model.AvailabilitySlot;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AvailabilitySlotDTO;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper bidireccional para convertir entre AvailabilitySlot (dominio) y AvailabilitySlotDTO (REST).
 * 
 * Responsabilidad:
 * - Conversión DTO → Dominio: Permite que los controladores conviertan DTOs entrantes a objetos de dominio
 * - Conversión Dominio → DTO: Permite convertir objetos de dominio a DTOs de respuesta
 * 
 * Este mapper sigue el mismo patrón que los PersistenceMappers del proyecto (bidireccionales),
 * manteniendo consistencia arquitectónica en toda la aplicación.
 * 
 * @author OdontoLink Team
 */
public class AvailabilitySlotInputMapper {

    private AvailabilitySlotInputMapper() {
        // Utility class
    }

    /**
     * Convierte un DTO de entrada a un Value Object de dominio.
     * Esta es una conversión simple sin lógica de negocio.
     * 
     * @param dto DTO desde la capa REST
     * @return Value Object de dominio
     */
    public static AvailabilitySlot toDomain(AvailabilitySlotDTO dto) {
        if (dto == null) {
            return null;
        }

        return new AvailabilitySlot(
                dto.getDayOfWeek(),
                dto.getStartTime(),
                dto.getEndTime()
        );
    }

    /**
     * Convierte un conjunto de DTOs a objetos de dominio.
     * 
     * @param dtos Set de DTOs desde la capa REST
     * @return Set de Value Objects de dominio
     */
    public static Set<AvailabilitySlot> toDomainSet(Set<AvailabilitySlotDTO> dtos) {
        if (dtos == null) {
            return null;
        }

        return dtos.stream()
                .map(AvailabilitySlotInputMapper::toDomain)
                .collect(Collectors.toSet());
    }

    /**
     * Convierte un Value Object de dominio a DTO de respuesta.
     * 
     * @param domain Value Object de dominio
     * @return DTO para la capa REST
     */
    public static AvailabilitySlotDTO toDTO(AvailabilitySlot domain) {
        if (domain == null) {
            return null;
        }

        return new AvailabilitySlotDTO(
                domain.getDayOfWeek(),
                domain.getStartTime(),
                domain.getEndTime()
        );
    }

    /**
     * Convierte un conjunto de objetos de dominio a DTOs.
     * 
     * @param domainSet Set de Value Objects de dominio
     * @return Set de DTOs para la capa REST
     */
    public static Set<AvailabilitySlotDTO> toDTOSet(Set<AvailabilitySlot> domainSet) {
        if (domainSet == null) {
            return null;
        }

        return domainSet.stream()
                .map(AvailabilitySlotInputMapper::toDTO)
                .collect(Collectors.toSet());
    }
}
