package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.AiAdminAuditEvent;

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
}
