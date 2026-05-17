package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.AiGovernancePolicy;

/**
 * Puerto de entrada para la administracion de la {@link AiGovernancePolicy}
 * (RF31). Permite al admin ajustar los pre-requisitos de publicacion del
 * agente y la posibilidad de override.
 */
public interface IAiGovernancePolicyUseCase {

    AiGovernancePolicy getPolicy();

    AiGovernancePolicy updatePolicy(boolean requireGuardrails,
                                    int minActiveGuardrails,
                                    boolean requireSystemPrompt,
                                    boolean requireWelcomeMessage,
                                    boolean requireIndexedDocuments,
                                    boolean allowOverride);
}
