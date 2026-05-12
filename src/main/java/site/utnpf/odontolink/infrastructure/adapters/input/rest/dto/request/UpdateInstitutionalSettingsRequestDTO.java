package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para que el administrador actualice los parámetros institucionales (RF07).
 *
 * Se exige el envío de la fotografía completa de campos editables. El
 * email de contacto se valida con {@link Email} para evitar guardar
 * direcciones malformadas que rompan los templates de comunicación.
 */
@Schema(description = "Carga completa de parámetros institucionales (RF07)")
public class UpdateInstitutionalSettingsRequestDTO {

    @Schema(description = "Nombre institucional visible en la plataforma",
            example = "Facultad de Odontología - UNT", required = true)
    @NotBlank(message = "El nombre institucional es obligatorio")
    @Size(max = 200, message = "El nombre institucional no puede superar los 200 caracteres")
    private String institutionName;

    @Schema(description = "Horarios de atención al público",
            example = "Lunes a Viernes de 08:00 a 18:00 hs")
    private String openingHours;

    @Schema(description = "Políticas de uso de la plataforma",
            example = "El uso de la plataforma implica la aceptación de los términos vigentes...")
    private String usagePolicies;

    @Schema(description = "Email institucional de contacto", example = "contacto@odontologia.unt.edu.ar")
    @Email(message = "El email de contacto debe ser válido")
    @Size(max = 150, message = "El email de contacto no puede superar los 150 caracteres")
    private String contactEmail;

    @Schema(description = "Teléfono institucional de contacto", example = "+54 381 4364093")
    @Size(max = 50, message = "El teléfono de contacto no puede superar los 50 caracteres")
    private String contactPhone;

    @Schema(description = "Dirección física de la institución", example = "Av. Benjamín Aráoz 800, San Miguel de Tucumán")
    @Size(max = 250, message = "La dirección no puede superar los 250 caracteres")
    private String contactAddress;

    public UpdateInstitutionalSettingsRequestDTO() {
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
}
