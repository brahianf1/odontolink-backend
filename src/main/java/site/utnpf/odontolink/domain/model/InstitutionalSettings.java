package site.utnpf.odontolink.domain.model;

import java.time.Instant;

/**
 * Configuración institucional de la plataforma (RF07).
 *
 * Se modela como un agregado singleton: existe a lo sumo una instancia
 * persistida en la base de datos, identificada con {@link #SINGLETON_ID}.
 * La elección de un singleton estructurado, en lugar de una tabla de
 * clave-valor, busca:
 * <ul>
 *   <li>tipado fuerte de cada parámetro institucional (nombre, horarios,
 *       políticas, contacto), sin necesidad de parsear strings genéricos;</li>
 *   <li>una sola operación de lectura para construir las pantallas
 *       públicas y del panel administrativo;</li>
 *   <li>idempotencia natural: una sola fila, una sola fuente de verdad.</li>
 * </ul>
 *
 * Las modificaciones aplicadas a este agregado se reflejan inmediatamente
 * en el sistema, tal como exige el RF07.
 */
public class InstitutionalSettings {

    /**
     * Identificador único de la fila singleton. Se asigna explícitamente
     * en lugar de delegar en el motor de autogeneración para garantizar
     * que jamás existan dos filas con configuración distinta.
     */
    public static final Long SINGLETON_ID = 1L;

    private Long id;
    private String institutionName;
    private String openingHours;
    private String usagePolicies;
    private String contactEmail;
    private String contactPhone;
    private String contactAddress;
    private Instant updatedAt;

    public InstitutionalSettings() {
    }

    public InstitutionalSettings(Long id,
                                 String institutionName,
                                 String openingHours,
                                 String usagePolicies,
                                 String contactEmail,
                                 String contactPhone,
                                 String contactAddress,
                                 Instant updatedAt) {
        this.id = id;
        this.institutionName = institutionName;
        this.openingHours = openingHours;
        this.usagePolicies = usagePolicies;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.contactAddress = contactAddress;
        this.updatedAt = updatedAt;
    }

    /**
     * Construye la configuración por defecto que se persiste la primera
     * vez que se accede al endpoint, evitando que el frontend reciba un
     * 404 antes de que el administrador realice la primera carga.
     *
     * Los valores son intencionadamente neutros y orientativos: la idea
     * es que el administrador los reemplace en cuanto entre por primera
     * vez al panel.
     */
    public static InstitutionalSettings defaults() {
        return new InstitutionalSettings(
                SINGLETON_ID,
                "OdontoLink",
                "Lunes a Viernes de 08:00 a 18:00 hs",
                "Pendiente de definición por el administrador.",
                "contacto@odontolink.local",
                "",
                "",
                Instant.now()
        );
    }

    /**
     * Aplica una modificación atómica sobre todos los campos editables del
     * agregado. Recibir todos los campos en una sola operación obliga al
     * llamante a enviar la fotografía completa, lo cual elimina ambigüedades
     * del tipo "¿este null significa borrar o no enviar?" que ensucian las
     * APIs de actualización parcial.
     */
    public void apply(String institutionName,
                      String openingHours,
                      String usagePolicies,
                      String contactEmail,
                      String contactPhone,
                      String contactAddress) {
        this.institutionName = institutionName;
        this.openingHours = openingHours;
        this.usagePolicies = usagePolicies;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.contactAddress = contactAddress;
        this.updatedAt = Instant.now();
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
