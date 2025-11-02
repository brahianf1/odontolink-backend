package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.springframework.stereotype.Component;
import site.utnpf.odontolink.domain.model.ChatSession;
import site.utnpf.odontolink.domain.model.Patient;
import site.utnpf.odontolink.domain.model.Practitioner;
import site.utnpf.odontolink.domain.repository.ChatSessionRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PatientEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.PractitionerEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaChatSessionRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.ChatSessionPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.PatientPersistenceMapper;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.PractitionerPersistenceMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de persistencia para ChatSession (Hexagonal Architecture).
 * Implementa la interfaz del dominio ChatSessionRepository usando JPA.
 * Puerto de salida (Output Adapter).
 *
 * Este adaptador maneja la persistencia de sesiones de chat y proporciona métodos
 * de consulta para los diferentes casos de uso del sistema de chat.
 *
 * @author OdontoLink Team
 */
@Component
public class ChatSessionPersistenceAdapter implements ChatSessionRepository {

    private final JpaChatSessionRepository jpaChatSessionRepository;

    public ChatSessionPersistenceAdapter(JpaChatSessionRepository jpaChatSessionRepository) {
        this.jpaChatSessionRepository = jpaChatSessionRepository;
    }

    @Override
    public ChatSession save(ChatSession chatSession) {
        var entity = ChatSessionPersistenceMapper.toEntity(chatSession);
        var savedEntity = jpaChatSessionRepository.save(entity);
        return ChatSessionPersistenceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ChatSession> findById(Long id) {
        return jpaChatSessionRepository.findById(id)
                .map(ChatSessionPersistenceMapper::toDomain);
    }

    @Override
    public List<ChatSession> findByPatient(Patient patient) {
        PatientEntity patientEntity = PatientPersistenceMapper.toEntity(patient);
        return jpaChatSessionRepository.findByPatient(patientEntity).stream()
                .map(ChatSessionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatSession> findByPractitioner(Practitioner practitioner) {
        PractitionerEntity practitionerEntity = PractitionerPersistenceMapper.toEntity(practitioner);
        return jpaChatSessionRepository.findByPractitioner(practitionerEntity).stream()
                .map(ChatSessionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPatientAndPractitioner(Patient patient, Practitioner practitioner) {
        PatientEntity patientEntity = PatientPersistenceMapper.toEntity(patient);
        PractitionerEntity practitionerEntity = PractitionerPersistenceMapper.toEntity(practitioner);
        return jpaChatSessionRepository.existsByPatientAndPractitioner(patientEntity, practitionerEntity);
    }

    @Override
    public Optional<ChatSession> findByPatientAndPractitioner(Patient patient, Practitioner practitioner) {
        PatientEntity patientEntity = PatientPersistenceMapper.toEntity(patient);
        PractitionerEntity practitionerEntity = PractitionerPersistenceMapper.toEntity(practitioner);
        return jpaChatSessionRepository.findByPatientAndPractitioner(patientEntity, practitionerEntity)
                .map(ChatSessionPersistenceMapper::toDomain);
    }

    @Override
    public List<ChatSession> findByPatientId(Long patientId) {
        return jpaChatSessionRepository.findByPatientId(patientId).stream()
                .map(ChatSessionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatSession> findByPractitionerId(Long practitionerId) {
        return jpaChatSessionRepository.findByPractitionerId(practitionerId).stream()
                .map(ChatSessionPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }
}
