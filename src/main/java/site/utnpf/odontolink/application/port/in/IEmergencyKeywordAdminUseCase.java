package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.EmergencyKeyword;

import java.util.List;

/**
 * Puerto de entrada del CRUD admin del diccionario de emergencias (RF32).
 *
 * <p>Modelo de gobernanza: el admin es responsable de mantener el lexico
 * actualizado. El sistema NO embebe defaults. La unica defensa es lifecycle
 * (publish del agente exige al menos N keywords si la policy asi lo decide,
 * pero esa parte se puede agregar despues).
 */
public interface IEmergencyKeywordAdminUseCase {

    List<EmergencyKeyword> listAll();

    EmergencyKeyword create(String term, boolean active);

    EmergencyKeyword update(Long id, String term, boolean active);

    void delete(Long id);
}
