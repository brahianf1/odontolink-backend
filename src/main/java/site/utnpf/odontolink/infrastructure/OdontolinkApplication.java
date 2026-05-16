package site.utnpf.odontolink.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Punto de entrada de la aplicación. Además de delegar en
 * {@link SpringApplication#run}, fija la zona horaria del JVM <b>antes</b> de
 * que Spring inicie el contexto.
 *
 * <p><b>Por qué fijarla en {@code main()} y no en {@code @PostConstruct}</b>:
 * en contenedores sin {@code TZ} (Ubuntu por defecto), el JVM arranca con
 * {@code GMT/UTC}. Si la zona se setea en {@code @PostConstruct}, hay una
 * ventana (entre el JVM start y la ejecución del callback) en la que
 * cualquier inicializador estático, log temprano o bean eager-cargado lee la
 * zona equivocada — basta con que una sola dependencia cachee {@code today}
 * para que comparadores como {@code offer_end_date >= today} excluyan filas
 * legítimas. Fijarla al principio de {@code main()} la deja correcta desde la
 * primera instrucción de la aplicación.
 *
 * <p><b>Defensa en profundidad</b>: el {@code Dockerfile} también declara
 * {@code ENV TZ=America/Argentina/Buenos_Aires}, lo que hace que el OS y el
 * propio JVM se inicien ya en la zona correcta sin depender de este código.
 * Este método queda como cinturón si el binario se ejecuta fuera de Docker.
 *
 * <p><b>Override</b>: respeta, por orden de precedencia,
 * {@code -Dodontolink.app.timezone=...}, {@code APP_TIMEZONE} y, en última
 * instancia, el ID por defecto. La zona se valida con
 * {@link ZoneId#of(String)} para fallar rápido si el ID es inválido en lugar
 * de caer silenciosamente a GMT.
 */
@SpringBootApplication
public class OdontolinkApplication {

    /**
     * Zona horaria operativa por defecto. Toda la lógica de calendario
     * ({@code LocalDate.now()}, validaciones de vigencia de ofertas, etc.) se
     * interpreta en esta zona. Cambiar este valor en runtime sin coordinar con
     * los datos rompe los comparadores de fechas.
     */
    private static final String DEFAULT_TIMEZONE = "America/Argentina/Buenos_Aires";

    public static void main(String[] args) {
        applyApplicationTimeZone();
        SpringApplication.run(OdontolinkApplication.class, args);
    }

    /**
     * Resuelve y fija la zona horaria efectiva del JVM. Se ejecuta como primera
     * instrucción del proceso para que ningún consumidor de {@code LocalDate.now()}
     * o {@code LocalDateTime.now()} pueda ver la zona equivocada.
     */
    private static void applyApplicationTimeZone() {
        String zoneId = resolveTimeZoneId();
        // Falla rápido si el ID es inválido: TimeZone.getTimeZone() silencia el
        // error devolviendo GMT, lo cual es la trampa exacta que este fix evita.
        ZoneId.of(zoneId);
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }

    private static String resolveTimeZoneId() {
        String fromProperty = System.getProperty("odontolink.app.timezone");
        if (isPresent(fromProperty)) {
            return fromProperty;
        }
        String fromEnv = System.getenv("APP_TIMEZONE");
        if (isPresent(fromEnv)) {
            return fromEnv;
        }
        return DEFAULT_TIMEZONE;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
