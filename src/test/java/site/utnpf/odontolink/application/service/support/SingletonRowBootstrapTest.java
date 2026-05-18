package site.utnpf.odontolink.application.service.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests del helper {@link SingletonRowBootstrap}.
 *
 * <p>Validamos los 3 caminos:
 * <ul>
 *   <li><b>Happy path</b>: el row ya existe y se devuelve sin tocar el saver.</li>
 *   <li><b>Lazy create</b>: el row no existe, se crea y se devuelve.</li>
 *   <li><b>Race de duplicate key</b>: el primer save lanza
 *       {@link DataIntegrityViolationException}, el helper refetchea y
 *       devuelve la fila creada por otro thread.</li>
 * </ul>
 *
 * <p>Usamos un {@link PlatformTransactionManager} stub que ejecuta los
 * callbacks de {@link org.springframework.transaction.support.TransactionTemplate}
 * sin abrir transacciones reales, suficiente para validar la logica.
 */
class SingletonRowBootstrapTest {

    private SingletonRowBootstrap helper;

    @BeforeEach
    void setUp() {
        helper = new SingletonRowBootstrap(new StubTransactionManager());
    }

    @Test
    void devuelveRowExistenteSinTocarSaver() {
        AtomicInteger saverCalls = new AtomicInteger();
        String result = helper.getOrCreate(
                () -> Optional.of("existing-row"),
                () -> "default-factory-called",
                row -> {
                    saverCalls.incrementAndGet();
                    return row;
                },
                "TestSingleton"
        );
        assertEquals("existing-row", result);
        assertEquals(0, saverCalls.get(),
                "Saver no deberia invocarse si el row ya existe.");
    }

    @Test
    void creaRowCuandoNoExiste() {
        AtomicInteger saverCalls = new AtomicInteger();
        String result = helper.getOrCreate(
                () -> Optional.empty(),
                () -> "fresh-default",
                row -> {
                    saverCalls.incrementAndGet();
                    return row + "-persisted";
                },
                "TestSingleton"
        );
        assertEquals("fresh-default-persisted", result);
        assertEquals(1, saverCalls.get());
    }

    @Test
    void recuperaTrasDuplicateKeyConRefetch() {
        // Simulamos race: el primer findSingleton devuelve empty (sigue camino
        // de save), el save lanza duplicate-key, el segundo findSingleton
        // devuelve la fila que otro thread creo.
        AtomicInteger finderCalls = new AtomicInteger();
        String result = helper.getOrCreate(
                () -> {
                    int call = finderCalls.incrementAndGet();
                    if (call == 1) {
                        return Optional.empty();
                    }
                    return Optional.of("row-created-by-other-thread");
                },
                () -> "would-be-default",
                row -> {
                    throw new DataIntegrityViolationException("Duplicate entry '1' for key 'PRIMARY'");
                },
                "TestSingleton"
        );
        assertEquals("row-created-by-other-thread", result);
        assertEquals(2, finderCalls.get(),
                "Debe llamar al finder dos veces: chequeo inicial + refetch tras duplicate-key.");
    }

    @Test
    void propagaErrorPatologicoCuandoRefetchTampocoEncuentra() {
        // Caso muy raro: duplicate-key + refetch vacio. La BD esta inconsistente.
        assertThrows(IllegalStateException.class, () -> helper.getOrCreate(
                Optional::empty,
                () -> "default",
                row -> {
                    throw new DataIntegrityViolationException("duplicate");
                },
                "TestSingleton"
        ));
    }

    /**
     * Stub minimo de PlatformTransactionManager: ejecuta el callback sin
     * abrir transaccion real. Suficiente para que TransactionTemplate
     * funcione en tests unitarios sin dependencia de BD.
     */
    private static class StubTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(
                org.springframework.transaction.TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            // no-op
        }

        @Override
        public void rollback(TransactionStatus status) {
            // no-op
        }
    }

    @Test
    void smokeStubManagerArranca() {
        // Garantiza que el stub no rompe cuando se intercala con cualquier
        // operacion del TransactionTemplate.
        StubTransactionManager mgr = new StubTransactionManager();
        SingletonRowBootstrap fresh = new SingletonRowBootstrap(mgr);
        assertTrue(fresh.getOrCreate(
                () -> Optional.of("ok"),
                () -> "x",
                row -> row,
                "Smoke"
        ).equals("ok"));
    }
}
