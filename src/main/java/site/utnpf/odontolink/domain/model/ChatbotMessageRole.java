package site.utnpf.odontolink.domain.model;

/**
 * Rol del autor de un mensaje del rolling buffer del chatbot.
 *
 * <p>Mapea 1:1 con los roles del wire format de chat completions de
 * DigitalOcean Gradient ({@code user} / {@code assistant}). El rol
 * {@code system} no vive en BD: se compone en runtime a partir de
 * {@code AiAgentConfiguration.composeInstruction(...)} y se antepone en cada
 * llamada al proveedor.
 */
public enum ChatbotMessageRole {
    USER,
    ASSISTANT
}
