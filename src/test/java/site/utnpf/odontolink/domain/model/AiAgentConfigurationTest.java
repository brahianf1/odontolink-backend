package site.utnpf.odontolink.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests del agregado {@link AiAgentConfiguration} (RF29/RF31/RF32/RF34).
 *
 * <p>Cobertura especifica: regresion del bug "EnumSet.copyOf(emptyCollection)
 * lanza IllegalArgumentException" reportado por el frontend al enviar
 * {@code allowedRoles: []} en modo PUBLIC.
 */
class AiAgentConfigurationTest {

    private AiAgentConfiguration baseAgent() {
        return AiAgentConfiguration.createNew(
                "Asistente OdontoLink",
                "Eres el asistente virtual de la clinica.",
                "Hola! Que necesitas?",
                new BigDecimal("0.7"),
                new BigDecimal("0.9"),
                256,
                10,
                AiRetrievalMethod.REWRITE
        );
    }

    @Test
    void applyChatbotConfigConAllowedRolesVacioNoExplota() {
        // Regresion BUG #2: el FE envia [] cuando accessMode != PRIVATE.
        AiAgentConfiguration cfg = baseAgent();
        cfg.applyChatbotConfig(
                AiAgentAccessMode.PUBLIC,
                Set.of(),
                AiPiiPolicy.BLOCK,
                20, 20, 60,
                "*** Emergencia ***",
                false
        );
        assertEquals(AiAgentAccessMode.PUBLIC, cfg.getAccessMode());
        assertTrue(cfg.getAllowedRoles().isEmpty());
    }

    @Test
    void applyChatbotConfigConAllowedRolesNullNoExplota() {
        AiAgentConfiguration cfg = baseAgent();
        cfg.applyChatbotConfig(
                AiAgentAccessMode.PUBLIC,
                null,
                AiPiiPolicy.BLOCK,
                20, 20, 60,
                "*** Emergencia ***",
                false
        );
        assertTrue(cfg.getAllowedRoles().isEmpty());
    }

    @Test
    void applyChatbotConfigConAllowedRolesUnmodifiableEmptySet() {
        // Otro shape que el FE podria enviar despues de deserializar
        AiAgentConfiguration cfg = baseAgent();
        cfg.applyChatbotConfig(
                AiAgentAccessMode.PUBLIC,
                Collections.emptySet(),
                AiPiiPolicy.BLOCK,
                20, 20, 60,
                "*** Emergencia ***",
                false
        );
        assertTrue(cfg.getAllowedRoles().isEmpty());
    }

    @Test
    void applyChatbotConfigConRolesPobladosFunciona() {
        AiAgentConfiguration cfg = baseAgent();
        cfg.applyChatbotConfig(
                AiAgentAccessMode.PRIVATE,
                EnumSet.of(Role.ROLE_PATIENT, Role.ROLE_PRACTITIONER),
                AiPiiPolicy.BLOCK,
                20, 20, 60,
                "*** Emergencia ***",
                false
        );
        assertEquals(2, cfg.getAllowedRoles().size());
        assertTrue(cfg.getAllowedRoles().contains(Role.ROLE_PATIENT));
        assertTrue(cfg.getAllowedRoles().contains(Role.ROLE_PRACTITIONER));
    }

    @Test
    void canBeUsedByAplicaReglasDeAcceso() {
        AiAgentConfiguration cfg = baseAgent();
        cfg.applyChatbotConfig(
                AiAgentAccessMode.PRIVATE,
                EnumSet.of(Role.ROLE_PATIENT),
                AiPiiPolicy.BLOCK,
                20, 20, 60,
                "*** Emergencia ***",
                false
        );
        assertTrue(cfg.canBeUsedBy(Role.ROLE_PATIENT));
        assertEquals(false, cfg.canBeUsedBy(Role.ROLE_PRACTITIONER));
        assertEquals(false, cfg.canBeUsedBy(null));
    }

    @Test
    void canBeUsedByEnPublicSiempreTrue() {
        AiAgentConfiguration cfg = baseAgent();
        cfg.applyChatbotConfig(
                AiAgentAccessMode.PUBLIC, Set.of(), AiPiiPolicy.BLOCK,
                20, 20, 60, "*** Emergencia ***", false);
        assertTrue(cfg.canBeUsedBy(null));
        assertTrue(cfg.canBeUsedBy(Role.ROLE_PATIENT));
    }

    @Test
    void canBeUsedByEnDisabledSiempreFalse() {
        AiAgentConfiguration cfg = baseAgent();
        cfg.applyChatbotConfig(
                AiAgentAccessMode.DISABLED, Set.of(), AiPiiPolicy.BLOCK,
                20, 20, 60, "*** Emergencia ***", false);
        assertEquals(false, cfg.canBeUsedBy(null));
        assertEquals(false, cfg.canBeUsedBy(Role.ROLE_ADMIN));
    }

    @Test
    void createNewArrancaConDefaultsDelChatbot() {
        AiAgentConfiguration cfg = baseAgent();
        // Los defaults se aplican antes del primer applyChatbotConfig.
        assertNotNull(cfg.getAccessMode());
        assertEquals(AiAgentAccessMode.DISABLED, cfg.getAccessMode());
        assertEquals(AiPiiPolicy.BLOCK, cfg.getPiiPolicy());
        assertEquals(20, cfg.getConversationBufferSize());
        assertEquals(20, cfg.getRateLimitAnonymousPerHour());
        assertEquals(60, cfg.getRateLimitAuthenticatedPerHour());
    }
}
