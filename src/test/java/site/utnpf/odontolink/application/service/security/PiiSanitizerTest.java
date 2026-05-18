package site.utnpf.odontolink.application.service.security;

import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.application.service.security.PiiSanitizer.PiiScanResult;
import site.utnpf.odontolink.domain.model.ChatbotPiiType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests del sanitizador de PII (RF31/RF32).
 *
 * <p>Cubrimos casos positivos por tipo + un par de false positives clasicos
 * (numeros aleatorios largos que no son tarjeta valida segun Luhn).
 */
class PiiSanitizerTest {

    private final PiiSanitizer sanitizer = new PiiSanitizer();

    @Test
    void detectaDniArgentinoConPuntos() {
        PiiScanResult r = sanitizer.scan("Mi DNI es 12.345.678 y quiero turno");
        assertTrue(r.hasPii());
        assertTrue(r.detected().contains(ChatbotPiiType.DNI));
        assertTrue(r.sanitized().contains("[DNI_REDACTADO]"));
    }

    @Test
    void detectaDniArgentinoSinPuntos() {
        PiiScanResult r = sanitizer.scan("DNI 12345678");
        assertTrue(r.hasPii());
        assertTrue(r.detected().contains(ChatbotPiiType.DNI));
    }

    @Test
    void detectaCuit() {
        PiiScanResult r = sanitizer.scan("Mi CUIT 20-12345678-9");
        assertTrue(r.hasPii());
        assertTrue(r.detected().contains(ChatbotPiiType.CUIT));
        assertTrue(r.sanitized().contains("[CUIT_REDACTADO]"));
    }

    @Test
    void detectaEmail() {
        PiiScanResult r = sanitizer.scan("Mandame info a juan.perez@example.com please");
        assertTrue(r.hasPii());
        assertTrue(r.detected().contains(ChatbotPiiType.EMAIL));
        assertTrue(r.sanitized().contains("[EMAIL_REDACTADO]"));
    }

    @Test
    void detectaTarjetaValidaPorLuhn() {
        // 4111 1111 1111 1111 es la tarjeta de prueba Visa, pasa Luhn
        PiiScanResult r = sanitizer.scan("Te paso mi tarjeta 4111 1111 1111 1111");
        assertTrue(r.hasPii());
        assertTrue(r.detected().contains(ChatbotPiiType.CREDIT_CARD));
        assertTrue(r.sanitized().contains("[TARJETA_REDACTADA]"));
    }

    @Test
    void ignoraNumeroLargoSinChecksumLuhn() {
        // 1234 5678 9012 3456 no pasa Luhn: NO debe marcarse como tarjeta.
        // Tampoco debe matchear como CBU (22 digitos). Es solo un numero largo.
        PiiScanResult r = sanitizer.scan("El codigo postal extendido es 1234 5678 9012 3456 pero no es nada");
        assertFalse(r.detected().contains(ChatbotPiiType.CREDIT_CARD));
    }

    @Test
    void detectaCbu() {
        PiiScanResult r = sanitizer.scan("CBU 0170120220000012345678 para transferir");
        assertTrue(r.hasPii());
        assertTrue(r.detected().contains(ChatbotPiiType.CBU));
    }

    @Test
    void detectaTelefonoAr() {
        PiiScanResult r = sanitizer.scan("Llamame al +54 9 11 12345678");
        assertTrue(r.hasPii());
        assertTrue(r.detected().contains(ChatbotPiiType.PHONE_AR));
    }

    @Test
    void textoLimpioNoDetectaNada() {
        PiiScanResult r = sanitizer.scan("Hola, queria saber el horario de la clinica.");
        assertFalse(r.hasPii());
        assertTrue(r.detected().isEmpty());
    }

    @Test
    void textoVacioNoRompe() {
        PiiScanResult r = sanitizer.scan("");
        assertFalse(r.hasPii());
    }

    @Test
    void luhnValidaVisaTestNumber() {
        assertTrue(PiiSanitizer.luhnValid("4111111111111111"));
    }

    @Test
    void luhnRechazaNumeroInvalido() {
        assertFalse(PiiSanitizer.luhnValid("1234567890123456"));
    }
}
