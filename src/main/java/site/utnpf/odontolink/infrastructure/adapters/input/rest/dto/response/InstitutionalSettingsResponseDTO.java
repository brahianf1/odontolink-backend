package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO de respuesta para los parámetros institucionales (RF07).
 *
 * El campo {@code updatedAt} se expone para que el frontend pueda mostrar
 * la última fecha de modificación y, eventualmente, implementar control
 * de concurrencia optimista en futuras iteraciones.
 */
@Schema(description = "Parámetros institucionales actuales (RF07)")
public class InstitutionalSettingsResponseDTO {

    @Schema(description = "Nombre institucional", example = "Facultad de Odontología - UNT")
    private String institutionName;

    @Schema(description = "Horarios de atención", example = "Lunes a Viernes de 08:00 a 18:00 hs")
    private String openingHours;

    @Schema(description = "Políticas de uso de la plataforma")
    private String usagePolicies;

    @Schema(description = "Email institucional de contacto", example = "contacto@odontologia.unt.edu.ar")
    private String contactEmail;

    @Schema(description = "Teléfono institucional de contacto", example = "+54 381 4364093")
    private String contactPhone;

    @Schema(description = "Dirección física", example = "Av. Benjamín Aráoz 800, San Miguel de Tucumán")
    private String contactAddress;

    @Schema(description = "Fecha de última modificación", example = "2026-05-12T17:42:11Z")
    private Instant updatedAt;

    public InstitutionalSettingsResponseDTO() {
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public String getUsagePolicies() {
        return usagePolicies;
    }

    public void setUsagePolicies(String usagePolicies) {
        this.usagePolicies = usagePolicies;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
