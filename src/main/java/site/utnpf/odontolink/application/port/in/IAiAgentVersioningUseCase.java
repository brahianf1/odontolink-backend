package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.model.AiAgentConfigurationVersion;
import site.utnpf.odontolink.domain.model.PageResult;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de entrada para el historial de versiones del agente y el audit
 * log (RF31). Ofrece lectura, rollback y consulta paginada del audit.
 */
public interface IAiAgentVersioningUseCase {

    List<AiAgentConfigurationVersion> listVersions();

    PageResult<AiAgentConfigurationVersion> listVersionsPaged(int page, int size);

    /**
     * Re-aplica una version anterior a la configuracion vigente y la
     * re-publica al proveedor. Genera una nueva version (no se reactiva
     * la antigua: se duplica el contenido como version nueva, lo que
     * deja un rastro claro en el historial).
     */
    AiAgentConfigurationVersion rollbackToVersion(int versionNumber);

    /**
     * Listado legacy del audit log: top-N en orden descendente cronologico.
     * Util cuando el caller solo quiere ver lo mas reciente sin paginacion.
     * El endpoint paginado preferido es {@link #listAuditEventsPaged}.
     */
    List<AiAdminAuditEvent> listAuditEvents(int limit);

    /**
     * Listado paginado del audit log con filtros opcionales por tipo y rango
     * temporal. El frontend lo usa para construir vistas auditables a 6+
     * meses sin saturar la red.
     */
    PageResult<AiAdminAuditEvent> listAuditEventsPaged(AiAdminAuditEvent.Type type,
                                                       Instant from,
                                                       Instant to,
                                                       int page,
                                                       int size);
}
