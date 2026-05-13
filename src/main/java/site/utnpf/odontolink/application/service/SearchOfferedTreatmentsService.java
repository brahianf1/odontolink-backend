package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.ISearchOfferedTreatmentsUseCase;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.OfferedTreatmentSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.domain.repository.OfferedTreatmentRepository;

/**
 * Servicio de aplicación delgado que orquesta la búsqueda paginada y filtrada
 * del catálogo público (RF09).
 *
 * No hay reglas de negocio adicionales en este nivel: la lógica de filtros
 * vive en el adaptador de persistencia (Specifications) y la lógica de
 * visibilidad (no exponer bajas lógicas) vive en el repositorio. Este
 * servicio se mantiene como una fina cáscara transaccional sólo para
 * encajar en la arquitectura hexagonal y permitir interceptar la operación
 * en el futuro con auditoría, métricas o caching sin reabrir el adaptador.
 */
@Transactional(readOnly = true)
public class SearchOfferedTreatmentsService implements ISearchOfferedTreatmentsUseCase {

    private final OfferedTreatmentRepository offeredTreatmentRepository;

    public SearchOfferedTreatmentsService(OfferedTreatmentRepository offeredTreatmentRepository) {
        this.offeredTreatmentRepository = offeredTreatmentRepository;
    }

    @Override
    public PageResult<OfferedTreatment> search(OfferedTreatmentSearchCriteria criteria, PageQuery pageQuery) {
        return offeredTreatmentRepository.search(criteria, pageQuery);
    }
}
