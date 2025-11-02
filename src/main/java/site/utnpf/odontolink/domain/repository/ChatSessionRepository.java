package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (Output Port) para operaciones de persistencia de ChatSession.
 * Sigue los principios de Arquitectura Hexagonal (Ports and Adapters).
 *
 * Este repositorio proporciona métodos para:
 * - Crear y guardar sesiones de chat
 * - Consultar sesiones por paciente o practicante
 * - Validar existencia de sesiones (para prevenir duplicados - RF27)
 * - Buscar la sesión específica entre un paciente y un practicante
 *
 * @author OdontoLink Team
 */
public interface ChatSessionRepository {

    /**
     * Guarda una nueva sesión de chat o actualiza una existente.
     *
     * @param chatSession La sesión de chat a guardar
     * @return La sesión de chat guardada con su ID asignado
     */
    ChatSession save(ChatSession chatSession);

    /**
     * Busca una sesión de chat por su ID.
     *
     * @param id El ID de la sesión de chat
     * @return Optional conteniendo la sesión si existe
     */
    Optional<ChatSession> findById(Long id);

    /**
     * Obtiene todas las sesiones de chat de un paciente.
     * Implementa CU 6.1: Obtener Lista de Sesiones de Chat.
     *
     * @param patient El paciente cuyas sesiones se quieren consultar
     * @return Lista de sesiones del paciente (vacía si no hay ninguna)
     */
    List<ChatSession> findByPatient(Patient patient);

    /**
     * Obtiene todas las sesiones de chat de un practicante.
     * Implementa CU 6.1: Obtener Lista de Sesiones de Chat.
     *
     * @param practitioner El practicante cuyas sesiones se quieren consultar
     * @return Lista de sesiones del practicante (vacía si no hay ninguna)
     */
    List<ChatSession> findByPractitioner(Practitioner practitioner);

    /**
     * Verifica si ya existe una sesión de chat entre un paciente y un practicante.
     * Implementa RF27: Restricción de chat sin relación previa.
     *
     * @param patient El paciente
     * @param practitioner El practicante
     * @return true si existe una sesión, false en caso contrario
     */
    boolean existsByPatientAndPractitioner(Patient patient, Practitioner practitioner);

    /**
     * Busca la sesión de chat específica entre un paciente y un practicante.
     *
     * @param patient El paciente
     * @param practitioner El practicante
     * @return Optional conteniendo la sesión si existe
     */
    Optional<ChatSession> findByPatientAndPractitioner(Patient patient, Practitioner practitioner);

    /**
     * Obtiene todas las sesiones de chat de un paciente por su ID.
     *
     * @param patientId El ID del paciente
     * @return Lista de sesiones del paciente
     */
    List<ChatSession> findByPatientId(Long patientId);

    /**
     * Obtiene todas las sesiones de chat de un practicante por su ID.
     *
     * @param practitionerId El ID del practicante
     * @return Lista de sesiones del practicante
     */
    List<ChatSession> findByPractitionerId(Long practitionerId);
}
