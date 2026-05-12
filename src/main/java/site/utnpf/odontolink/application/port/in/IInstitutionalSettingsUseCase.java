package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.InstitutionalSettings;

/**
 * Puerto de entrada para los parámetros institucionales (RF07).
 *
 * El requisito demanda que las modificaciones se apliquen de forma
 * inmediata, por lo que ambas operaciones son síncronas y no requieren
 * jobs ni eventos adicionales.
 */
public interface IInstitutionalSettingsUseCase {

    /**
     * Devuelve la configuración vigente. Si el agregado no existe todavía
     * (estado inicial post-deploy), la implementación debe crearlo con
     * valores por defecto antes de devolverlo, evitando 404 espurios.
     */
    InstitutionalSettings getSettings();

    /**
     * Actualiza el agregado con la fotografía completa de campos. Se
     * exige una actualización total (no parcial) para forzar al cliente
     * a tomar siempre decisiones explícitas sobre cada parámetro
     * institucional.
     */
    InstitutionalSettings updateSettings(String institutionName,
                                         String openingHours,
                                         String usagePolicies,
                                         String contactEmail,
                                         String contactPhone,
                                         String contactAddress);
}
