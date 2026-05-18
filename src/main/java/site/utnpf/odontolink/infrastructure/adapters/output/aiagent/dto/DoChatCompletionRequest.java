package site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload de {@code POST {AGENT_URL}/api/v1/chat/completions?agent=true} para
 * DO Gradient.
 *
 * <p>El wire format es deliberadamente igual al de OpenAI chat completions
 * porque DO lo expone bajo el mismo contrato. {@code stream:false} fija el
 * modo sincronico que nuestro adapter usa con RestClient blocking.
 *
 * <p>{@code model} es opcional: cuando se llama al endpoint del agente con
 * {@code ?agent=true} el modelo lo provee el agente deployado en DO. Lo
 * exponemos como opcional por si el operador necesita forzarlo (override) en
 * algun entorno; en operacion normal queda null y se omite del JSON.
 *
 * <p>El campo {@code include_retrieval_info} se elimino: no figura en la
 * documentacion oficial de DO Gradient para este endpoint y empezo a causar
 * timeouts cuando lo enviabamos. Si en el futuro DO documenta un parametro
 * equivalente para devolver los chunks RAG usados (necesario para el
 * indicador de confianza RF34), agregamoslo con el nombre correcto.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DoChatCompletionRequest(
        @JsonProperty("messages") List<DoMessage> messages,
        @JsonProperty("stream") boolean stream,
        @JsonProperty("model") String model
) {
}
