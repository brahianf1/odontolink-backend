package site.utnpf.odontolink.infrastructure.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.DigitalOceanAgentPlatformProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Valida al arranque que las credenciales del modulo IA esten presentes y
 * emite WARNs cuando faltan. No aborta la aplicacion: el resto del backend
 * sigue funcionando aunque el chatbot quede degradado.
 *
 * <p>Motivacion: en el incidente de produccion del 18/05, el chatbot devolvia
 * fallback en todos los mensajes porque el endpoint del agente respondia 401
 * sin body. La causa raiz era que el cliente HTTP estaba enviando el Personal
 * Access Token (PAT) al endpoint del agente, en lugar de la Access Key
 * dedicada del agente. Si el operador hubiera visto un WARN claro al arranque
 * ("DIGITALOCEAN_AGENT_ACCESS_KEY no configurada"), el bug habria sido obvio.
 *
 * <p>Lista de credenciales que valida:
 * <ul>
 *   <li>{@code DIGITALOCEAN_ACCESS_TOKEN} (management PAT).</li>
 *   <li>{@code DIGITALOCEAN_AGENT_UUID} (UUID del agente).</li>
 *   <li>{@code DIGITALOCEAN_KNOWLEDGE_BASE_UUID} (UUID de la KB).</li>
 *   <li>{@code DIGITALOCEAN_AGENT_ACCESS_KEY} (key de invocacion del agente).</li>
 * </ul>
 *
 * <p>La URL de invocacion ({@code DIGITALOCEAN_AGENT_INVOCATION_URL}) NO se
 * valida aqui porque es opcional (puede descubrirse via management).
 */
@Component
public class AiAgentCredentialsValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiAgentCredentialsValidator.class);

    private final DigitalOceanAgentPlatformProperties props;

    public AiAgentCredentialsValidator(DigitalOceanAgentPlatformProperties props) {
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> missing = new ArrayList<>();
        if (isBlank(props.getAccessToken())) {
            missing.add("DIGITALOCEAN_ACCESS_TOKEN");
        }
        if (isBlank(props.getAgentUuid())) {
            missing.add("DIGITALOCEAN_AGENT_UUID");
        }
        if (isBlank(props.getKnowledgeBaseUuid())) {
            missing.add("DIGITALOCEAN_KNOWLEDGE_BASE_UUID");
        }
        if (isBlank(props.getAgentInvocationAccessKey())) {
            missing.add("DIGITALOCEAN_AGENT_ACCESS_KEY");
        }

        if (missing.isEmpty()) {
            log.info("Credenciales del modulo IA presentes. Management + invocacion habilitados.");
            return;
        }
        // No abortamos: otros modulos no dependen del modulo IA. Pero el WARN
        // queda visible en startup para que el operador detecte el problema
        // antes que un usuario reporte un fallback del chatbot.
        log.warn("Modulo IA: faltan credenciales {}. El chatbot y/o el modulo admin de IA "
                        + "responderan con errores hasta que se completen las ENV. "
                        + "Resto del backend opera con normalidad.",
                missing);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
