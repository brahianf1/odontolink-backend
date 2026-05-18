package site.utnpf.odontolink.application.service.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Helper de uso comun para "find or create" de filas singleton (id fijo)
 * resistente a race conditions entre transacciones concurrentes.
 *
 * <p>Problema que resuelve: cuando un endpoint del admin hace lazy bootstrap
 * (find singleton; si no existe, save defaults), dos requests en paralelo
 * pueden ver {@code findSingleton()} vacio simultaneamente y ambas intentar
 * insertar el row id=1 — la segunda revienta con {@code Duplicate entry '1'
 * for key PRIMARY} y un 500 al cliente. Esto pasaba con
 * {@code AiGovernancePolicy} cuando el frontend del modulo IA disparaba en
 * paralelo {@code /configuration}, {@code /governance}, {@code /health}.
 *
 * <p>Estrategia: el path eager (ver {@code AiAgentSingletonBootstrapper})
 * deberia haber sembrado las filas al arranque, asi que este helper se
 * ejecuta solo en el caso degradado de borrado manual post-bootstrap. Para
 * cubrir ese caso aun bajo concurrencia, abrimos una transaccion nueva
 * {@link TransactionDefinition#PROPAGATION_REQUIRES_NEW} en cada intento:
 * <ul>
 *   <li>Si falla con {@link DataIntegrityViolationException}, la transaccion
 *       interna se rollbackea sin afectar a la externa, y reintentamos un
 *       refetch en otra transaccion nueva: hay alguien mas que ya creo el
 *       row y solo necesitamos leerlo.</li>
 *   <li>Si el segundo refetch tampoco encuentra el row, propagamos
 *       {@link IllegalStateException} (caso patologico).</li>
 * </ul>
 *
 * <p>Es defensivo: en operacion normal nunca se invoca porque el row ya
 * existe. Si se invoca, garantiza idempotencia.
 *
 * <p><b>Registro</b>: el {@code @SpringBootApplication} del proyecto vive en
 * {@code site.utnpf.odontolink.infrastructure}, asi que el component scan
 * solo cubre ese paquete. Esta clase queda en {@code application.service.support}
 * (donde semanticamente pertenece) y se expone como {@code @Bean} explicito
 * en {@code BeanConfiguration}, alineada al patron del resto del proyecto.
 */
public class SingletonRowBootstrap {

    private static final Logger log = LoggerFactory.getLogger(SingletonRowBootstrap.class);

    private final TransactionTemplate requiresNew;

    public SingletonRowBootstrap(PlatformTransactionManager transactionManager) {
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Devuelve la fila singleton; la crea con {@code defaultFactory} si no
     * existe. Idempotente y seguro contra race.
     *
     * @param finder        funcion que lee la fila (devuelve Optional).
     * @param defaultFactory funcion que produce el agregado con valores default.
     * @param saver         funcion que persiste el agregado.
     * @param resourceLabel etiqueta humana para logs (e.g. "AiGovernancePolicy").
     * @param <T>           tipo del agregado.
     */
    public <T> T getOrCreate(Supplier<Optional<T>> finder,
                             Supplier<T> defaultFactory,
                             java.util.function.Function<T, T> saver,
                             String resourceLabel) {
        // Lectura fresca en transaccion nueva: garantiza que vemos el estado
        // mas reciente de la BD (no estamos atados al snapshot de la tx caller).
        Optional<T> existing = requiresNew.execute(status -> finder.get());
        if (existing != null && existing.isPresent()) {
            return existing.get();
        }
        // No existe: intentamos crearlo en otra tx nueva.
        try {
            T created = requiresNew.execute(status -> saver.apply(defaultFactory.get()));
            log.info("{}: fila singleton creada por lazy fallback.", resourceLabel);
            return created;
        } catch (DataIntegrityViolationException ex) {
            // Otra request concurrente nos gano. Refetcheamos.
            log.warn("{}: race detectada en lazy bootstrap; refetcheando.", resourceLabel);
            Optional<T> refetched = requiresNew.execute(status -> finder.get());
            return refetched.orElseThrow(() -> new IllegalStateException(
                    resourceLabel + ": duplicate-key on save pero refetch vacio. "
                            + "La BD esta en estado inconsistente.", ex));
        }
    }
}
