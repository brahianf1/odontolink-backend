package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.ProviderGuardrail;

import java.util.List;

/**
 * Puerto de entrada para gestion de guardrails nativos del proveedor (RF31).
 *
 * <p>Operaciones distintas a las de {@link IAgentPolicyRuleAdminUseCase}:
 * los guardrails del proveedor NO se crean con texto custom (la API del
 * proveedor no lo permite), solo se vinculan/desvinculan + se ajusta su
 * prioridad. La metadata fina (categorias del Sensitive Data, default
 * response) es solo editable en el dashboard del proveedor.
 */
public interface IProviderGuardrailAdminUseCase {

    /**
     * Lista los guardrails del proveedor conocidos localmente. Si la lista
     * esta vacia o desactualizada, el admin puede invocar {@link #refreshFromProvider()}
     * para sincronizar el espejo local con lo que el proveedor reporta.
     */
    List<ProviderGuardrail> listGuardrails();

    /**
     * Refresca el espejo local consultando al proveedor: agrega los
     * guardrails nuevos que aparecieron en la cuenta, actualiza metadata
     * descriptiva de los existentes. NO modifica la intencion de attach
     * que el admin haya configurado localmente.
     *
     * @return el catalogo actualizado.
     */
    List<ProviderGuardrail> refreshFromProvider();

    /**
     * Actualiza la intencion de attach + priority de un guardrail. El cambio
     * se reflejara en el proveedor en el proximo {@code publish()} del
     * agente.
     */
    ProviderGuardrail updateAttachment(Long id, boolean attached, int priority);

    /**
     * Snapshot inmutable que orienta al admin cuando el espejo local de
     * guardrails esta vacio. Existe porque la API publica del proveedor no
     * expone un endpoint para listar el catalogo standalone (solo
     * attach/detach contra un agente).
     */
    record BootstrapInfo(
            boolean catalogEmpty,
            String providerName,
            String providerDashboardUrl,
            String instructionsText) {
    }

    /**
     * Devuelve info para guiar el bootstrap manual desde el dashboard del
     * proveedor. La UI la consume cuando {@link #listGuardrails()} viene
     * vacio para renderizar un banner accionable con link directo.
     */
    BootstrapInfo getBootstrapInfo();
}
