package site.utnpf.odontolink.infrastructure.adapters.output.aiagent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import site.utnpf.odontolink.application.port.out.ILlmAgentProviderPort.GuardrailAttachment;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoAttachGuardrailsRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests del adapter management de DO (no confundir con el adapter de
 * invocacion del chat).
 *
 * <p>Foco: regresion del bug productivo donde {@code attachGuardrail} mandaba
 * body singular {@code {guardrail_uuid, priority}} y DO respondia 400. La
 * API real es batch (operationId {@code genai_attach_agent_guardrails}):
 * espera un POST con shape {@code {guardrails: [{guardrail_uuid, priority}, ...]}}.
 */
class DigitalOceanLlmAgentAdapterTest {

    private static final String AGENT_UUID = "agent-uuid-123";

    private DigitalOceanGradientClient client;
    private DigitalOceanLlmAgentAdapter adapter;

    @BeforeEach
    void setUp() {
        client = mock(DigitalOceanGradientClient.class);
        adapter = new DigitalOceanLlmAgentAdapter(client);
    }

    @Test
    void attachGuardrailsEnviaBodyBatchConArrayGuardrails() {
        List<GuardrailAttachment> attachments = List.of(
                new GuardrailAttachment("uuid-1", 1),
                new GuardrailAttachment("uuid-2", 5));

        adapter.attachGuardrails(AGENT_UUID, attachments);

        ArgumentCaptor<DoAttachGuardrailsRequest> bodyCaptor =
                ArgumentCaptor.forClass(DoAttachGuardrailsRequest.class);
        verify(client).post(eq("/v2/gen-ai/agents/" + AGENT_UUID + "/guardrails"),
                bodyCaptor.capture(), eq(Object.class));

        DoAttachGuardrailsRequest body = bodyCaptor.getValue();
        assertNotNull(body);
        assertNotNull(body.guardrails());
        assertEquals(2, body.guardrails().size());
        // El shape DEBE ser {guardrails: [{guardrail_uuid, priority}, ...]} (batch).
        assertEquals("uuid-1", body.guardrails().get(0).guardrailUuid());
        assertEquals(1, body.guardrails().get(0).priority());
        assertEquals("uuid-2", body.guardrails().get(1).guardrailUuid());
        assertEquals(5, body.guardrails().get(1).priority());
    }

    @Test
    void attachGuardrailsConListaVaciaNoLlamaAlClient() {
        // Sin nada que mandar evitamos un POST con body vacio que DO
        // podria rechazar como 400.
        adapter.attachGuardrails(AGENT_UUID, List.of());
        verify(client, never()).post(any(), any(), any());
    }

    @Test
    void attachGuardrailsConListaNullNoLlamaAlClient() {
        adapter.attachGuardrails(AGENT_UUID, null);
        verify(client, never()).post(any(), any(), any());
    }

    @Test
    void detachGuardrailUsaPathSingular() {
        adapter.detachGuardrail(AGENT_UUID, "uuid-xyz");
        verify(client).delete("/v2/gen-ai/agents/" + AGENT_UUID + "/guardrails/uuid-xyz");
    }

    @Test
    void attachBatchSerializableUsaSnakeCaseEnLasPropiedades() throws Exception {
        // Verificamos que Jackson serializa con snake_case via @JsonProperty.
        // Esto es lo que DO espera en el wire.
        DoAttachGuardrailsRequest body = new DoAttachGuardrailsRequest(List.of(
                new DoAttachGuardrailsRequest.GuardrailItem("u1", 7)));
        com.fasterxml.jackson.databind.ObjectMapper om =
                new com.fasterxml.jackson.databind.ObjectMapper();
        String json = om.writeValueAsString(body);
        assertTrue(json.contains("\"guardrails\""), "json debe tener clave 'guardrails'");
        assertTrue(json.contains("\"guardrail_uuid\""), "json debe usar snake_case 'guardrail_uuid'");
        assertTrue(json.contains("\"priority\""), "json debe incluir priority");
        assertTrue(json.contains("\"u1\""));
        assertTrue(json.contains("7"));
    }
}
