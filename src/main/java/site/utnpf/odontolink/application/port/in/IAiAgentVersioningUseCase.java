package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.model.AiAgentConfigurationVersion;

import java.util.List;

/**
 * Puerto de entrada para el historial de versiones del agente y el audit
 * log (RF31). Ofrece lectura y rollback.
 */
public interface IAiAgentVersioningUseCase {

    List<AiAgentConfigurationVersion> listVersions();

    /**
     * Re-aplica una version anterior a la configuracion vigente y la
     * re-publica al proveedor. Genera una nueva version (no se reactiva
     * la antigua: se duplica el contenido como version nueva, lo que
     * deja un rastro claro en el historial).
     */
    AiAgentConfigurationVersion rollbackToVersion(int versionNumber);

    List<AiAdminAuditEvent> listAuditEvents(int limit);
}
