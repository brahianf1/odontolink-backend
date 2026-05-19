package site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response;

/**
 * Informacion para guiar al admin a hacer bootstrap manual del catalogo
 * de guardrails del proveedor (RF31).
 *
 * <p>Existe porque la API publica de DigitalOcean Gradient NO expone un
 * endpoint para listar el catalogo standalone de guardrails: el array
 * {@code apiAgent.guardrails} solo contiene los que ya estan attached al
 * agente. Por eso, si el admin nunca attacheo nada desde el dashboard de
 * DO, nuestro refresh no tiene como descubrir los UUIDs disponibles.
 *
 * <p>El FE consulta este endpoint cuando el listado local viene vacio para
 * ofrecerle al admin instrucciones claras + link directo al dashboard del
 * proveedor.
 */
public class ProviderGuardrailBootstrapInfoResponseDTO {

    /** {@code true} si el espejo local esta vacio y se necesita bootstrap manual. */
    private boolean catalogEmpty;

    /** Nombre legible del proveedor para mostrar en la UI. */
    private String providerName;

    /**
     * URL del dashboard del proveedor donde el admin puede vincular guardrails
     * manualmente al agente. {@code null} si el agente no esta configurado.
     */
    private String providerDashboardUrl;

    /**
     * Texto explicativo en espanol para mostrar al admin en el banner de
     * bootstrap. El backend lo provee para mantener la copia consistente con
     * la realidad del contrato (la UI solo lo renderiza).
     */
    private String instructionsText;

    public ProviderGuardrailBootstrapInfoResponseDTO() {
    }

    public boolean isCatalogEmpty() {
        return catalogEmpty;
    }

    public void setCatalogEmpty(boolean catalogEmpty) {
        this.catalogEmpty = catalogEmpty;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderDashboardUrl() {
        return providerDashboardUrl;
    }

    public void setProviderDashboardUrl(String providerDashboardUrl) {
        this.providerDashboardUrl = providerDashboardUrl;
    }

    public String getInstructionsText() {
        return instructionsText;
    }

    public void setInstructionsText(String instructionsText) {
        this.instructionsText = instructionsText;
    }
}
