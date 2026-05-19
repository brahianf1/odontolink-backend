package site.utnpf.odontolink.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort.AgentSnapshot;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort.ProviderGuardrailSnapshot;
import site.utnpf.odontolink.domain.model.AiRetrievalMethod;
import site.utnpf.odontolink.domain.model.ProviderGuardrail;
import site.utnpf.odontolink.domain.model.ProviderGuardrailType;
import site.utnpf.odontolink.domain.repository.AiAgentConfigurationRepository;
import site.utnpf.odontolink.domain.repository.ProviderGuardrailRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests del refresh autoritativo (RF31).
 *
 * <p>Foco: regresion del bug productivo donde {@code refreshFromProvider}
 * solo actualizaba metadata descriptiva y dejaba intactos {@code attached}
 * y {@code priority} de los locales que ya existian. Eso hacia que cualquier
 * cambio que el admin hiciera en el dashboard de DO directamente no se
 * reflejara en nuestro panel.
 */
class ProviderGuardrailAdminServiceTest {

    private static final String AGENT_UUID = "agent-uuid-123";

    private ProviderGuardrailRepository repo;
    private AiAgentConfigurationRepository configRepo;
    private ILlmAgentProviderPort llm;
    private ProviderGuardrailAdminService service;

    @BeforeEach
    void setUp() {
        repo = mock(ProviderGuardrailRepository.class);
        configRepo = mock(AiAgentConfigurationRepository.class);
        llm = mock(ILlmAgentProviderPort.class);
        service = new ProviderGuardrailAdminService(repo, configRepo, llm, AGENT_UUID);
    }

    @Test
    void refreshSobreescribeAttachedYPriorityDeLosExistentes() {
        // Estado local: un guardrail con intent attached=false, priority=100.
        ProviderGuardrail local = new ProviderGuardrail(
                1L, "uuid-A", ProviderGuardrailType.JAILBREAK, "Jailbreak", "desc",
                false, 100, "default", Instant.now(), Instant.now());

        // DO reporta el mismo guardrail PERO attached=true, priority=5 (el admin
        // lo activo desde el dashboard externamente).
        ProviderGuardrailSnapshot remote = new ProviderGuardrailSnapshot(
                "uuid-A", ProviderGuardrailType.JAILBREAK, "Jailbreak", "desc",
                "default", true, 5);

        when(repo.findByProviderGuardrailUuid("uuid-A")).thenReturn(Optional.of(local));
        when(repo.findAllOrderByPriorityAsc())
                .thenReturn(List.of(local))   // primer paso del refresh
                .thenReturn(List.of(local));  // segundo paso (return final)
        when(llm.getAgent(AGENT_UUID)).thenReturn(snapshotWith(List.of(remote)));
        when(repo.save(any(ProviderGuardrail.class))).thenAnswer(i -> i.getArgument(0));

        service.refreshFromProvider();

        // El local debe haber sido actualizado a la realidad de DO.
        assertTrue(local.isAttached(), "refresh debe pisar el intent local con lo que reporta DO");
        assertEquals(5, local.getPriority(), "refresh debe actualizar priority desde DO");
    }

    @Test
    void refreshMarcaAttachedFalseSiDoNoReportaElGuardrail() {
        // Estado local: un guardrail attached=true (intent del admin).
        ProviderGuardrail local = new ProviderGuardrail(
                1L, "uuid-X", ProviderGuardrailType.SENSITIVE_DATA, "PII", "desc",
                true, 10, "default", Instant.now(), Instant.now());

        // DO no reporta NADA: el guardrail dejo de estar disponible para este agente.
        when(repo.findAllOrderByPriorityAsc()).thenReturn(List.of(local));
        when(llm.getAgent(AGENT_UUID)).thenReturn(snapshotWith(List.of()));
        when(repo.save(any(ProviderGuardrail.class))).thenAnswer(i -> i.getArgument(0));

        service.refreshFromProvider();

        assertFalse(local.isAttached(),
                "Si DO no reporta el guardrail, el espejo local debe quedar attached=false");
        // Priority se preserva por si vuelve a aparecer en un refresh futuro.
        assertEquals(10, local.getPriority(),
                "Priority debe preservarse aunque attached pase a false");
    }

    @Test
    void refreshAgregaGuardrailesNuevosDelProveedor() {
        // No hay locales existentes; DO reporta uno nuevo attached.
        ProviderGuardrailSnapshot remote = new ProviderGuardrailSnapshot(
                "uuid-NEW", ProviderGuardrailType.CONTENT_MODERATION, "Moderation",
                "desc", "blocked", true, 50);

        when(repo.findByProviderGuardrailUuid("uuid-NEW")).thenReturn(Optional.empty());

        // Capturamos lo guardado para verificar el shape del nuevo.
        List<ProviderGuardrail> saved = new ArrayList<>();
        when(repo.save(any(ProviderGuardrail.class))).thenAnswer(i -> {
            ProviderGuardrail g = i.getArgument(0);
            saved.add(g);
            return g;
        });
        when(repo.findAllOrderByPriorityAsc()).thenAnswer(i -> new ArrayList<>(saved));
        when(llm.getAgent(AGENT_UUID)).thenReturn(snapshotWith(List.of(remote)));

        service.refreshFromProvider();

        assertFalse(saved.isEmpty(), "Debe haberse guardado al menos un guardrail nuevo");
        ProviderGuardrail nuevo = saved.get(0);
        assertEquals("uuid-NEW", nuevo.getProviderGuardrailUuid());
        assertEquals(ProviderGuardrailType.CONTENT_MODERATION, nuevo.getType());
        assertTrue(nuevo.isAttached(), "El nuevo debe arrancar con el attached que reporta DO");
        assertEquals(50, nuevo.getPriority());
    }

    private AgentSnapshot snapshotWith(List<ProviderGuardrailSnapshot> guardrails) {
        return new AgentSnapshot(
                AGENT_UUID, "instr", null, null, 256, 5,
                AiRetrievalMethod.NONE, Instant.now(), "url", false, guardrails);
    }
}
