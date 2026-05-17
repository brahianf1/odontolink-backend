package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;
import site.utnpf.odontolink.domain.model.PageResult;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de salida para los eventos de auditoria del modulo IA (RF31).
 */
public interface AiAdminAuditEventRepository {

    AiAdminAuditEvent save(AiAdminAuditEvent event);

    /**
     * Devuelve los eventos en orden descendente cronologico. El admin
     * los ve mas recientes primero al revisar el historial.
     */
    List<AiAdminAuditEvent> findAllOrderByOccurredAtDesc(int limit);

    /**
     * Devuelve una pagina filtrada de eventos. Todos los filtros son
     * opcionales y se combinan con AND:
     * <ul>
     *   <li>{@code type}: tipo exacto del evento.</li>
     *   <li>{@code from}: solo eventos con {@code occurredAt} mayor o igual.</li>
     *   <li>{@code to}: solo eventos con {@code occurredAt} menor (half-open).</li>
     * </ul>
     */
    PageResult<AiAdminAuditEvent> findPaged(AiAdminAuditEvent.Type type,
                                            Instant from,
                                            Instant to,
                                            int page,
                                            int size);
}
