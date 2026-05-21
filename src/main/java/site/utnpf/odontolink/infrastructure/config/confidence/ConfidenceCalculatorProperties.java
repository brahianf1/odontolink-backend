package site.utnpf.odontolink.infrastructure.config.confidence;

import org.springframework.boot.context.properties.ConfigurationProperties;
import site.utnpf.odontolink.domain.model.ConfidenceCalculatorConfig;

import java.util.List;
import java.util.Objects;

/**
 * Configuracion del calculador de confianza del chatbot (RF34).
 *
 * <p>Vinculada al prefijo {@code odontolink.chatbot.confidence}. Todos los
 * parametros estan exteriorizados a properties — el dominio NO hardcodea
 * pesos, umbrales ni mensajes. Esto permite calibrar el sistema con datos
 * empiricos (Exp 2/3/4 del PoC y validaciones posteriores con expertos)
 * sin tocar codigo.
 *
 * <p>Defaults derivados del PoC:
 * <ul>
 *   <li>{@code normalizationFactor=10.0}: los scores de DO Gradient se
 *       observaron en rango [0.69, 12.87]. Dividir por 10 mapea el centro
 *       de masa a ~0.5 y satura a 1.0 los muy relevantes.</li>
 *   <li>{@code weightRetrieval=0.7, weightCoverage=0.3}: el juez (S_B) se
 *       descarto por no ser confiable con el mismo agente. Cuando se agregue
 *       en una fase futura, los pesos se redistribuyen.</li>
 *   <li>{@code strongRetrievalThreshold=0.4, strongCoverageThreshold=0.5}:
 *       elegidos contra el set de 28 preguntas para que ~50% de las in-domain
 *       caigan en categoria OFFICIAL y el resto en PARTIAL.</li>
 *   <li>{@code alpha} de no-RAG=0.5 y short-reply=0.7: penalizaciones
 *       heuristicas documentadas en el plan tecnico.</li>
 * </ul>
 *
 * <p>Los patrones de refusal y los mensajes por categoria viven aqui porque
 * son configurables sin redeploy (al editar properties + restart). Si el FE
 * necesita variantes (ej. tono distinto por canal), pueden agregarse claves
 * adicionales sin tocar el dominio.
 */
@ConfigurationProperties(prefix = "odontolink.chatbot.confidence")
public class ConfidenceCalculatorProperties {

    /** Factor de normalizacion para mapear el raw score de DO a [0, 1]. */
    private double normalizationFactor = 10.0;

    /** Pesos relativos por senal. NO se valida que sumen 1.0; documentado en domain. */
    private double weightRetrieval = 0.7;
    private double weightCoverage = 0.3;

    /** Umbrales para clasificar como OFFICIAL: basta con superar uno (OR). */
    private double strongRetrievalThreshold = 0.4;
    private double strongCoverageThreshold = 0.5;

    /** Umbral para que un chunk cuente como "relevante" en la cobertura. */
    private double chunkRelevanceThreshold = 0.4;

    /** Cantidad de chunks "esperados" para saturar S_C a 1.0. */
    private int expectedRelevantChunks = 3;

    /** Pesos del top-k para el promedio ponderado de S_A. */
    private List<Double> topKWeights = List.of(0.5, 0.3, 0.2);

    /** Penalizaciones multiplicativas alpha. */
    private double alphaNoRetrieval = 0.5;
    private double alphaShortReply = 0.7;

    /** Umbral debajo del cual una respuesta cuenta como "muy corta". */
    private int shortReplyThresholdChars = 30;

    /**
     * Patrones (substring, lowercase, sin tildes) que indican que la respuesta
     * fue un rechazo o evasiva. La logica de matching normaliza el reply
     * antes de comparar para tolerar variantes ortograficas.
     */
    private List<String> refusalPatterns = List.of(
            "no puedo procesar esta solicitud",
            "esta consulta ha sido filtrada",
            "razones de seguridad",
            "no puedo ayudarte con",
            "no puedo responder preguntas que no",
            "no puedo proporcionar informacion",
            "no estoy disenado para",
            "no tengo informacion sobre",
            "no tengo acceso a esa",
            "esta fuera del alcance",
            "esta fuera de mi alcance",
            "fuera del scope",
            "mi funcion esta exclusivamente",
            "exclusivamente relacionada con",
            "mi ambito de informacion",
            "mi rol esta estrictamente",
            "mi rol esta enfocado",
            "esta estrictamente enfocado",
            "esta enfocado exclusivamente",
            "estrictamente enfocado en la plataforma",
            "como asistente oficial",
            "se especializa exclusivamente en",
            "se centra exclusivamente en",
            "te sugiero que busques en fuentes",
            "no se trata a traves de",
            "no corresponde al ambito"
    );

    /**
     * Subgrupo dedicado a los textos mostrados al paciente por cada categoria
     * de confianza. Permite ajustar el copy sin tocar codigo.
     */
    private Messages messages = new Messages();

    public double getNormalizationFactor() { return normalizationFactor; }
    public void setNormalizationFactor(double normalizationFactor) { this.normalizationFactor = normalizationFactor; }

    public double getWeightRetrieval() { return weightRetrieval; }
    public void setWeightRetrieval(double weightRetrieval) { this.weightRetrieval = weightRetrieval; }

    public double getWeightCoverage() { return weightCoverage; }
    public void setWeightCoverage(double weightCoverage) { this.weightCoverage = weightCoverage; }

    public double getStrongRetrievalThreshold() { return strongRetrievalThreshold; }
    public void setStrongRetrievalThreshold(double strongRetrievalThreshold) { this.strongRetrievalThreshold = strongRetrievalThreshold; }

    public double getStrongCoverageThreshold() { return strongCoverageThreshold; }
    public void setStrongCoverageThreshold(double strongCoverageThreshold) { this.strongCoverageThreshold = strongCoverageThreshold; }

    public double getChunkRelevanceThreshold() { return chunkRelevanceThreshold; }
    public void setChunkRelevanceThreshold(double chunkRelevanceThreshold) { this.chunkRelevanceThreshold = chunkRelevanceThreshold; }

    public int getExpectedRelevantChunks() { return expectedRelevantChunks; }
    public void setExpectedRelevantChunks(int expectedRelevantChunks) { this.expectedRelevantChunks = expectedRelevantChunks; }

    public List<Double> getTopKWeights() { return topKWeights; }
    public void setTopKWeights(List<Double> topKWeights) {
        // Defensa: si el operador pasa una lista vacia, conservamos el default
        // razonable para no dejar el calculador sin pesos.
        this.topKWeights = (topKWeights == null || topKWeights.isEmpty())
                ? List.of(0.5, 0.3, 0.2) : List.copyOf(topKWeights);
    }

    public double getAlphaNoRetrieval() { return alphaNoRetrieval; }
    public void setAlphaNoRetrieval(double alphaNoRetrieval) { this.alphaNoRetrieval = alphaNoRetrieval; }

    public double getAlphaShortReply() { return alphaShortReply; }
    public void setAlphaShortReply(double alphaShortReply) { this.alphaShortReply = alphaShortReply; }

    public int getShortReplyThresholdChars() { return shortReplyThresholdChars; }
    public void setShortReplyThresholdChars(int shortReplyThresholdChars) { this.shortReplyThresholdChars = shortReplyThresholdChars; }

    public List<String> getRefusalPatterns() { return refusalPatterns; }
    public void setRefusalPatterns(List<String> refusalPatterns) {
        this.refusalPatterns = (refusalPatterns == null || refusalPatterns.isEmpty())
                ? List.of() : List.copyOf(refusalPatterns);
    }

    public Messages getMessages() { return messages; }
    public void setMessages(Messages messages) { this.messages = Objects.requireNonNullElseGet(messages, Messages::new); }

    /**
     * Convierte este DTO de properties en el record de dominio
     * {@link ConfidenceCalculatorConfig}. Es el unico cruce de capas
     * permitido: infraestructura mapea a dominio, no al reves.
     */
    public ConfidenceCalculatorConfig toDomainConfig() {
        return new ConfidenceCalculatorConfig(
                normalizationFactor,
                weightRetrieval,
                weightCoverage,
                strongRetrievalThreshold,
                strongCoverageThreshold,
                chunkRelevanceThreshold,
                expectedRelevantChunks,
                topKWeights,
                alphaNoRetrieval,
                alphaShortReply,
                shortReplyThresholdChars,
                refusalPatterns,
                new ConfidenceCalculatorConfig.CategoryMessages(
                        messages.getOfficialLabel(),
                        messages.getOfficialBody(),
                        messages.getPartialLabel(),
                        messages.getPartialBody(),
                        messages.getGeneralLabel(),
                        messages.getGeneralBody(),
                        messages.getOutOfScopeLabel(),
                        messages.getOutOfScopeBody()
                )
        );
    }

    /**
     * Textos que ve el paciente. Configurables por entorno (ej. demo vs
     * produccion con tono distinto).
     */
    public static class Messages {

        private String officialLabel = "Información oficial";
        private String officialBody =
                "Esta respuesta está basada en documentos oficiales de la Facultad de Odontología UNT.";

        private String partialLabel = "Información parcial";
        private String partialBody =
                "Encontré información relacionada en nuestros documentos institucionales, pero puede no responder " +
                        "completamente tu consulta. Si es importante, confirmá con la secretaría de la facultad.";

        private String generalLabel = "Respuesta general";
        private String generalBody =
                "Esta respuesta es de conocimiento general y no está respaldada por nuestros documentos. " +
                        "Si necesitás precisión, confirmá con la secretaría de la facultad.";

        private String outOfScopeLabel = "Fuera de alcance";
        private String outOfScopeBody =
                "Tu consulta no corresponde al ámbito de OdontoLink (turnos, tratamientos y procesos académicos de la FOUNT).";

        public String getOfficialLabel() { return officialLabel; }
        public void setOfficialLabel(String officialLabel) { this.officialLabel = officialLabel; }
        public String getOfficialBody() { return officialBody; }
        public void setOfficialBody(String officialBody) { this.officialBody = officialBody; }

        public String getPartialLabel() { return partialLabel; }
        public void setPartialLabel(String partialLabel) { this.partialLabel = partialLabel; }
        public String getPartialBody() { return partialBody; }
        public void setPartialBody(String partialBody) { this.partialBody = partialBody; }

        public String getGeneralLabel() { return generalLabel; }
        public void setGeneralLabel(String generalLabel) { this.generalLabel = generalLabel; }
        public String getGeneralBody() { return generalBody; }
        public void setGeneralBody(String generalBody) { this.generalBody = generalBody; }

        public String getOutOfScopeLabel() { return outOfScopeLabel; }
        public void setOutOfScopeLabel(String outOfScopeLabel) { this.outOfScopeLabel = outOfScopeLabel; }
        public String getOutOfScopeBody() { return outOfScopeBody; }
        public void setOutOfScopeBody(String outOfScopeBody) { this.outOfScopeBody = outOfScopeBody; }
    }
}
