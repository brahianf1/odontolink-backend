package site.utnpf.odontolink.application.service;

import org.springframework.transaction.annotation.Transactional;
import site.utnpf.odontolink.application.port.in.IInstitutionalSettingsUseCase;
import site.utnpf.odontolink.domain.model.InstitutionalSettings;
import site.utnpf.odontolink.domain.repository.InstitutionalSettingsRepository;

/**
 * Servicio de aplicación que implementa el caso de uso de configuración
 * institucional (RF07).
 *
 * Estrategia "lazy bootstrap": la fila singleton se crea con valores por
 * defecto la primera vez que cualquiera (admin o consumidor anónimo
 * autorizado) accede a la configuración. Esto evita acoplarse a una
 * migración SQL inicial y mantiene la semántica de "siempre existe una
 * configuración" para el resto del sistema.
 *
 * Todas las operaciones son transaccionales: la lectura usa una
 * transacción de sólo lectura para liberar bloqueos rápidamente, y la
 * actualización ejecuta lectura + escritura dentro de la misma
 * transacción para que la modificación sea atómica.
 */
@Transactional
public class InstitutionalSettingsService implements IInstitutionalSettingsUseCase {

    private final InstitutionalSettingsRepository repository;

    public InstitutionalSettingsService(InstitutionalSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    public InstitutionalSettings getSettings() {
        // Lazy bootstrap: si nadie inicializó la configuración aún, persistimos
        // un singleton con valores por defecto. Mantiene el contrato "siempre
        // hay configuración" sin depender de scripts externos de seed.
        return repository.findSingleton()
                .orElseGet(() -> repository.save(InstitutionalSettings.defaults()));
    }

    @Override
    public InstitutionalSettings updateSettings(String institutionName,
                                                String openingHours,
                                                String usagePolicies,
                                                String contactEmail,
                                                String contactPhone,
                                                String contactAddress) {
        // Reutilizamos getSettings() para resolver el caso de "primera carga
        // del administrador": si todavía no hay fila, se crea con defaults y
        // luego se aplica la modificación. El resultado neto es una sola
        // persistencia final con los valores deseados.
        InstitutionalSettings settings = getSettings();
        settings.apply(
                institutionName,
                openingHours,
                usagePolicies,
                contactEmail,
                contactPhone,
                contactAddress
        );
        return repository.save(settings);
    }
}
