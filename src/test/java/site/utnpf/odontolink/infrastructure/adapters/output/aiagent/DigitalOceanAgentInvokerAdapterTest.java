package site.utnpf.odontolink.infrastructure.adapters.output.aiagent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort.ChatMessage;
import site.utnpf.odontolink.application.port.out.ILlmAgentInvokerPort.ProbeResult;
import site.utnpf.odontolink.domain.exception.LlmProviderException;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoChatCompletionRequest;
import site.utnpf.odontolink.infrastructure.adapters.output.aiagent.dto.DoChatCompletionResponse;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del adapter de invocacion del agente DO.
 *
 * <p>Foco principal en la regresion del bug productivo: la URL invocada debe
 * incluir {@code ?agent=true} (el endpoint requiere ese flag, sin el
 * timing-out). Tambien validamos que el body NO incluya el campo
 * {@code include_retrieval_info} (no documentado, causaba el timeout) y que
 * el {@code model} se omite cuando esta vacio.
 */
class DigitalOceanAgentInvokerAdapterTest {

    private DigitalOceanGradientClient invocationClient;
    private DigitalOceanGradientClient probeClient;
    private DigitalOceanAgentInvokerAdapter adapter;

    @BeforeEach
    void setUp() {
        invocationClient = mock(DigitalOceanGradientClient.class);
        probeClient = mock(DigitalOceanGradientClient.class);
        adapter = new DigitalOceanAgentInvokerAdapter(invocationClient, probeClient, "");
    }

    @Test
    void invokeAgregaAgentTrueAlUrl() {
        when(invocationClient.postAbsolute(anyString(), any(), eq(DoChatCompletionResponse.class)))
                .thenReturn(emptyResponse());

        adapter.invoke("https://abc.agents.do-ai.run",
                List.of(new ChatMessage("user", "hola")));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(invocationClient).postAbsolute(urlCaptor.capture(), any(), any());
        assertEquals("https://abc.agents.do-ai.run/api/v1/chat/completions?agent=true",
                urlCaptor.getValue());
    }

    @Test
    void invokeRespetaTrailingSlash() {
        when(invocationClient.postAbsolute(anyString(), any(), eq(DoChatCompletionResponse.class)))
                .thenReturn(emptyResponse());

        adapter.invoke("https://abc.agents.do-ai.run/",
                List.of(new ChatMessage("user", "hola")));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(invocationClient).postAbsolute(urlCaptor.capture(), any(), any());
        assertEquals("https://abc.agents.do-ai.run/api/v1/chat/completions?agent=true",
                urlCaptor.getValue());
    }

    @Test
    void invokeOmiteModelCuandoNoHayOverride() {
        when(invocationClient.postAbsolute(anyString(), any(), eq(DoChatCompletionResponse.class)))
                .thenReturn(emptyResponse());

        adapter.invoke("https://abc.agents.do-ai.run",
                List.of(new ChatMessage("user", "hola")));

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(invocationClient).postAbsolute(anyString(), bodyCaptor.capture(), any());
        DoChatCompletionRequest body = (DoChatCompletionRequest) bodyCaptor.getValue();
        assertNull(body.model(), "model debe omitirse cuando no hay override");
        assertFalse(body.stream(), "stream debe ser false (modo sincronico)");
        assertEquals(1, body.messages().size());
    }

    @Test
    void invokeIncluyeModelSiHayOverride() {
        DigitalOceanAgentInvokerAdapter adapterConModelo =
                new DigitalOceanAgentInvokerAdapter(invocationClient, probeClient, "llama3-8b-instruct");
        when(invocationClient.postAbsolute(anyString(), any(), eq(DoChatCompletionResponse.class)))
                .thenReturn(emptyResponse());

        adapterConModelo.invoke("https://abc.agents.do-ai.run",
                List.of(new ChatMessage("user", "hola")));

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(invocationClient).postAbsolute(anyString(), bodyCaptor.capture(), any());
        DoChatCompletionRequest body = (DoChatCompletionRequest) bodyCaptor.getValue();
        assertEquals("llama3-8b-instruct", body.model());
    }

    @Test
    void invokeConUrlVaciaLanzaErrorEspecifico() {
        LlmProviderException ex = assertThrows(LlmProviderException.class,
                () -> adapter.invoke("", List.of(new ChatMessage("user", "hola"))));
        assertEquals("AI_AGENT_INVOCATION_URL_UNAVAILABLE", ex.getErrorCode());
    }

    @Test
    void probeUsaProbeClientNoInvocationClient() {
        when(probeClient.postAbsolute(anyString(), any(), eq(DoChatCompletionResponse.class)))
                .thenReturn(emptyResponse());

        ProbeResult result = adapter.probe("https://abc.agents.do-ai.run");

        assertTrue(result.reachable());
        verify(probeClient).postAbsolute(anyString(), any(), any());
    }

    @Test
    void probeConTimeoutDevuelveDetalleClaro() {
        when(probeClient.postAbsolute(anyString(), any(), eq(DoChatCompletionResponse.class)))
                .thenThrow(new ResourceAccessException("read timed out",
                        new SocketTimeoutException("Read timed out")));

        ProbeResult result = adapter.probe("https://abc.agents.do-ai.run");

        assertFalse(result.reachable());
        assertTrue(result.errorDetail().contains("Timeout"),
                "El detail debe mencionar Timeout explicitamente para diagnostico");
    }

    @Test
    void probeConUrlVaciaDevuelveErrorPredictible() {
        ProbeResult result = adapter.probe(null);
        assertFalse(result.reachable());
        assertEquals("URL del agente no configurada.", result.errorDetail());
    }

    private DoChatCompletionResponse emptyResponse() {
        return new DoChatCompletionResponse(
                "test-id", "test-model", List.of(), null, null);
    }
}
