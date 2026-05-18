package site.utnpf.odontolink.application.service.security;

import site.utnpf.odontolink.domain.model.EmergencyKeyword;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Detector local de emergencias en el mensaje del usuario del chatbot (RF32).
 *
 * <p>Se invoca antes de enviar el mensaje al proveedor. Si matchea, el use
 * case marca {@code emergencyDetected=true} en la respuesta y antepone un
 * banner fijo con derivacion a guardia/emergencias. Es defensa en
 * profundidad: aunque el LLM ignore el guardrail clinico, el FE recibe
 * siempre una flag estructurada.
 *
 * <p>Por que no NLP / embeddings: el lexico de emergencias odontologicas es
 * pequenio y curado por el admin. Un matcher por boundaries con normalizacion
 * de acentos cubre las variaciones legitimas (Sangrado, SANGRADO, sangrado)
 * sin allocar modelos pesados.
 *
 * <p>El cache de keywords activos no vive aqui: lo hace el adapter de
 * persistencia para que el detector siga siendo stateless y testeable.
 */
public class EmergencyDetector {

    /**
     * Devuelve {@code true} si el {@code input} normalizado contiene al menos
     * uno de los terminos del catalogo. Devuelve {@code false} para input
     * vacio o coleccion vacia (no es una "emergencia" un mensaje sin keywords).
     */
    public boolean containsEmergencyTerm(String input, Collection<EmergencyKeyword> activeKeywords) {
        if (input == null || input.isBlank() || activeKeywords == null || activeKeywords.isEmpty()) {
            return false;
        }
        String normalizedInput = EmergencyKeyword.normalize(input);
        for (EmergencyKeyword kw : activeKeywords) {
            if (!kw.isActive()) {
                continue;
            }
            String term = kw.getNormalizedTerm();
            if (term.isEmpty()) {
                continue;
            }
            // Boundary-aware contains: usamos regex \b para que "dolor" no
            // matchee "duelo lor" pero si "dolor agudo" o "dolor.". Quoteamos
            // el termino para evitar inyeccion de metacaracteres regex desde
            // el admin (defensivo).
            String regex = "\\b" + Pattern.quote(term) + "\\b";
            if (Pattern.compile(regex).matcher(normalizedInput).find()) {
                return true;
            }
        }
        return false;
    }
}
