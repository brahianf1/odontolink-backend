package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO de request para vinculación múltiple de practicantes.
 * Utilizado en operaciones batch para vincular varios practicantes de una sola vez.
 */
@Schema(description = "Request para vincular múltiples practicantes a un supervisor")
public class BatchLinkPractitionersRequestDTO {

    @NotNull(message = "La lista de IDs de practicantes no puede ser nula")
    @NotEmpty(message = "La lista de IDs de practicantes no puede estar vacía")
    @Schema(
        description = "Lista de IDs de los practicantes a vincular",
        example = "[1, 2, 3, 4, 5]",
        required = true
    )
    private List<Long> practitionerIds;

    // Constructores
    public BatchLinkPractitionersRequestDTO() {
    }

    public BatchLinkPractitionersRequestDTO(List<Long> practitionerIds) {
        this.practitionerIds = practitionerIds;
    }

    // Getters y Setters
    public List<Long> getPractitionerIds() {
        return practitionerIds;
    }

    public void setPractitionerIds(List<Long> practitionerIds) {
        this.practitionerIds = practitionerIds;
    }
}
