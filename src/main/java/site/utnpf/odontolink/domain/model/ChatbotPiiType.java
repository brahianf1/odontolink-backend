package site.utnpf.odontolink.domain.model;

/**
 * Tipos de datos sensibles que el sanitizador local intenta detectar antes
 * de enviar un mensaje al proveedor LLM (RF31, RF32).
 *
 * <p>La lista se enfoca en el contexto argentino (DNI, CUIT/CUIL, CBU,
 * telefonos AR). Los tipos no-AR (email, tarjetas globales con Luhn) tambien
 * se cubren porque tienen patrones bien establecidos.
 *
 * <p>Cuando el {@code PiiSanitizer} detecta uno o mas tipos, el use case
 * decide segun {@link AiPiiPolicy} si bloquea el envio o reemplaza los matches
 * por placeholders y envia el texto sanitizado.
 */
public enum ChatbotPiiType {
    DNI,
    CUIT,
    CBU,
    CREDIT_CARD,
    EMAIL,
    PHONE_AR
}
