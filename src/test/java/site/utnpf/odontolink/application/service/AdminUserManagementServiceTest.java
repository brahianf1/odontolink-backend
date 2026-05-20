package site.utnpf.odontolink.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.utnpf.odontolink.application.port.in.IPatientRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.IPractitionerRegistrationUseCase;
import site.utnpf.odontolink.application.port.in.ISupervisorRegistrationUseCase;
import site.utnpf.odontolink.domain.exception.InvalidBusinessRuleException;
import site.utnpf.odontolink.domain.exception.ResourceNotFoundException;
import site.utnpf.odontolink.domain.model.Role;
import site.utnpf.odontolink.domain.model.User;
import site.utnpf.odontolink.domain.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre las reglas anti-lockout del management administrativo de usuarios:
 *
 * <ul>
 *   <li>Self-deactivation: un admin no puede desactivarse a sí mismo.</li>
 *   <li>Last-admin-standing: no se puede desactivar al único admin activo.</li>
 *   <li>Happy path: un admin puede desactivar a otro admin cuando hay ≥ 2
 *       activos en el sistema, y a usuarios no-admin sin restricciones de
 *       conteo.</li>
 *   <li>updateUserProfile sigue rechazando targets ADMIN: la regla del
 *       producto sobre edición cruzada entre admins no cambia.</li>
 * </ul>
 *
 * Estilo mockista, alineado con el resto de tests de servicio del proyecto:
 * los efectos de persistencia se observan a través de verify() y se
 * confirma que el agregado quedó en el estado esperado antes del save.
 */
class AdminUserManagementServiceTest {

    private UserRepository userRepository;
    private AdminUserManagementService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        IPatientRegistrationUseCase patientUseCase = mock(IPatientRegistrationUseCase.class);
        IPractitionerRegistrationUseCase practitionerUseCase = mock(IPractitionerRegistrationUseCase.class);
        ISupervisorRegistrationUseCase supervisorUseCase = mock(ISupervisorRegistrationUseCase.class);
        service = new AdminUserManagementService(
                userRepository, patientUseCase, practitionerUseCase, supervisorUseCase
        );
    }

    @Test
    @DisplayName("self-deactivation: admin que intenta darse de baja a sí mismo recibe 422")
    void rejectsSelfDeactivation() {
        User admin = activeUser(1L, Role.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        InvalidBusinessRuleException ex = assertThrows(InvalidBusinessRuleException.class,
                () -> service.deactivateUser(1L, admin));
        assertContains(ex.getMessage(), "propia cuenta");

        verify(userRepository, never()).save(any());
        // No malgastamos un round-trip al contador si ya rechazamos por self-check.
        verify(userRepository, never()).countActiveByRole(any());
    }

    @Test
    @DisplayName("last-admin-standing: no se puede desactivar al único admin activo aunque sea otro")
    void rejectsWhenTargetIsLastActiveAdmin() {
        User actingAdmin = activeUser(1L, Role.ROLE_ADMIN);
        User otherAdmin = activeUser(2L, Role.ROLE_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countActiveByRole(Role.ROLE_ADMIN)).thenReturn(1L);

        InvalidBusinessRuleException ex = assertThrows(InvalidBusinessRuleException.class,
                () -> service.deactivateUser(2L, actingAdmin));
        assertContains(ex.getMessage(), "administrador");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("happy path admin→admin: con 2+ admins activos, la baja procede")
    void deactivatesAnotherAdminWhenQuorumExists() {
        User actingAdmin = activeUser(1L, Role.ROLE_ADMIN);
        User otherAdmin = activeUser(2L, Role.ROLE_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countActiveByRole(Role.ROLE_ADMIN)).thenReturn(2L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.deactivateUser(2L, actingAdmin);

        assertFalse(result.isActive(),
                "el target debe quedar isActive=false tras la operación exitosa");
        verify(userRepository).save(otherAdmin);
    }

    @Test
    @DisplayName("happy path admin→no-admin: no consulta el contador de admins")
    void deactivatesNonAdminWithoutCheckingAdminCount() {
        User actingAdmin = activeUser(1L, Role.ROLE_ADMIN);
        User patient = activeUser(99L, Role.ROLE_PATIENT);
        when(userRepository.findById(99L)).thenReturn(Optional.of(patient));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.deactivateUser(99L, actingAdmin);

        assertFalse(result.isActive());
        verify(userRepository, never()).countActiveByRole(any());
        verify(userRepository).save(patient);
    }

    @Test
    @DisplayName("target inexistente → 404 ResourceNotFoundException")
    void notFoundWhenTargetMissing() {
        when(userRepository.findById(500L)).thenReturn(Optional.empty());
        User actingAdmin = activeUser(1L, Role.ROLE_ADMIN);

        assertThrows(ResourceNotFoundException.class,
                () -> service.deactivateUser(500L, actingAdmin));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("desactivar un admin YA inactivo: el chequeo last-admin lo deja pasar y el dominio rechaza")
    void alreadyInactiveAdminBlockedByDomainRule() {
        User actingAdmin = activeUser(1L, Role.ROLE_ADMIN);
        User inactiveAdmin = inactiveUser(2L, Role.ROLE_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveAdmin));

        InvalidBusinessRuleException ex = assertThrows(InvalidBusinessRuleException.class,
                () -> service.deactivateUser(2L, actingAdmin));
        // El mensaje proviene de User#deactivate, mapeado a InvalidBusinessRuleException.
        assertContains(ex.getMessage(), "inactivo");
        // El contador de admins no se debe consultar: el target ya está inactivo,
        // su baja no cambiaría el conteo y delegamos al dominio el "ya inactivo".
        verify(userRepository, never()).countActiveByRole(any());
    }

    @Test
    @DisplayName("updateUserProfile sigue rechazando targets ADMIN")
    void updateProfileOnAdminStillRejected() {
        User admin = activeUser(2L, Role.ROLE_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        InvalidBusinessRuleException ex = assertThrows(InvalidBusinessRuleException.class,
                () -> service.updateUserProfile(2L, "Nuevo", "Nombre", "999", LocalDate.now().minusYears(40)));
        assertContains(ex.getMessage(), "administrador");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("reactivateUser sobre admin inactivo procede sin necesidad de guards")
    void reactivateAdminProceedsWithoutGuards() {
        User inactiveAdmin = inactiveUser(2L, Role.ROLE_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactiveAdmin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.reactivateUser(2L);

        assertEquals(true, result.isActive());
        verify(userRepository).save(inactiveAdmin);
    }

    // ----- helpers -----

    private User activeUser(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setActive(true);
        return u;
    }

    private User inactiveUser(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setActive(false);
        return u;
    }

    private void assertContains(String haystack, String needle) {
        assert haystack != null && haystack.toLowerCase().contains(needle.toLowerCase())
                : "Mensaje no contiene '" + needle + "': " + haystack;
    }
}
