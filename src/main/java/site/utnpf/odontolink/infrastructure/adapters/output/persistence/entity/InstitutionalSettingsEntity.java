package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code institutional_settings} (RF07).
 *
 * Se asigna un identificador fijo en la capa de aplicación
 * ({@link site.utnpf.odontolink.domain.model.InstitutionalSettings#SINGLETON_ID})
 * para garantizar que la tabla nunca contenga más de una fila. Se evita el
 * uso de {@code @GeneratedValue} precisamente porque queremos imponer el
 * singleton a nivel de modelo, no depender de validaciones externas.
 *
 * Los campos de texto largo (políticas de uso, horarios) se mapean como
 * {@code TEXT} para no acotar artificialmente la longitud que el
 * administrador puede ingresar.
 */
@Entity
@Table(name = "institutional_settings")
public class InstitutionalSettingsEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "institution_name", nullable = false, length = 200)
    private String institutionName;

    @Column(name = "opening_hours", columnDefinition = "TEXT")
    private String openingHours;

    @Column(name = "usage_policies", columnDefinition = "TEXT")
    private String usagePolicies;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_address", length = 250)
    private String contactAddress;

    /**
     * Cuántos turnos SCHEDULED simultáneos puede tener un paciente dentro
     * de una misma Atención. Persistir el límite junto al resto de los
     * parámetros institucionales permite que el administrador lo ajuste
     * con el mismo flujo (PUT singleton) que el resto de la configuración.
     *
     * Se declara con {@code nullable = false} y un valor por defecto a
     * nivel de columna para que los entornos donde la tabla ya exista
     * puedan migrar sin requerir un script de backfill explícito: las
     * filas previas heredan automáticamente el límite por defecto.
     */
    @Column(name = "max_concurrent_appointments_per_attention",
            nullable = false,
            columnDefinition = "INT NOT NULL DEFAULT 1")
    private int maxConcurrentAppointmentsPerAttention;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InstitutionalSettingsEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getMaxConcurrentAppointmentsPerAttention() {
        return maxConcurrentAppointmentsPerAttention;
    }

    public void setMaxConcurrentAppointmentsPerAttention(int maxConcurrentAppointmentsPerAttention) {
        this.maxConcurrentAppointmentsPerAttention = maxConcurrentAppointmentsPerAttention;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
