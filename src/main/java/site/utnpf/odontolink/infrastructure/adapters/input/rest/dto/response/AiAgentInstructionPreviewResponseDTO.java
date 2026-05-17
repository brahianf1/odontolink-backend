package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Respuesta del preview de instruction: contiene el texto final concatenado
 * y la lista de labels de guardrails activos que se anteponen.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiAgentInstructionPreviewResponseDTO(
        String composedInstruction,
        List<String> activeGuardrailLabels
) {
}
