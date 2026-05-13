package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.OfferedTreatmentSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;

/**
 * Puerto de entrada para el motor de búsqueda del catálogo público (RF09).
 *
 * Separado de {@link IOfferedTreatmentUseCase} y de {@link IAppointmentUseCase}
 * para respetar el principio de Segregación de Interfaces: este caso de uso
 * no comparte dependencias con la gestión del catálogo personal del practicante
 * ni con la reserva de turnos. Tener un puerto dedicado documenta su
 * responsabilidad y simplifica los tests del adaptador REST público.
 */
public interface ISearchOfferedTreatmentsUseCase {

    /**
     * Ejecuta una búsqueda paginada y filtrada sobre el catálogo público.
     *
     * Sólo se retornan ofertas activas; la baja lógica de RF16 las excluye
     * de cualquier listado dirigido a pacientes.
     *
     * @param criteria  Criterios opcionales (keyword/specialty/availability)
     * @param pageQuery Paginación y ordenamiento solicitados
     * @return Página de resultados con metadata
     */
    PageResult<OfferedTreatment> search(OfferedTreatmentSearchCriteria criteria, PageQuery pageQuery);
}
