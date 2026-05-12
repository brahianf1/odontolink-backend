# syntax=docker/dockerfile:1.7

# =============================================================================
# Etapa 1 — Build
#   Usa la imagen oficial Eclipse Temurin JDK 17 (multiarquitectura, incluye
#   linux/arm64). Construye el jar fat de Spring Boot y lo extrae en capas
#   para que la imagen de runtime pueda copiar cada capa por separado y
#   reutilizar mejor la caché.
# =============================================================================
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

# Copiamos primero el wrapper de Maven y el descriptor del proyecto para
# maximizar los aciertos de caché en la resolución de dependencias cuando
# sólo cambian archivos fuente.
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

RUN chmod +x mvnw \
    && ./mvnw -B -ntp dependency:go-offline

# Copiamos las fuentes y compilamos. Los tests se omiten aquí porque CI los
# corre antes del despliegue; el build del contenedor se enfoca en producir
# el artefacto desplegable.
COPY src src

RUN ./mvnw -B -ntp clean package -DskipTests \
    && mkdir -p target/extracted \
    && java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted


# =============================================================================
# Etapa 2 — Runtime
#   Usa Eclipse Temurin JRE 17 (multiarquitectura, linux/arm64 incluida).
#   Corre con un usuario sin privilegios, expone la aplicación en el puerto
#   8080 y reporta su salud vía Spring Boot Actuator.
# =============================================================================
FROM eclipse-temurin:17-jre-jammy AS runtime

# curl es necesario para el HEALTHCHECK; tini se encarga del manejo correcto
# de señales como PID 1 para que la JVM reciba SIGTERM y haga un apagado
# ordenado en los redeploys de Dokploy.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tini \
    && rm -rf /var/lib/apt/lists/*

# Usuario sin privilegios para el runtime.
RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring --home-dir /app --shell /usr/sbin/nologin spring

WORKDIR /app

# Copiamos las capas de Spring Boot en orden de frecuencia de cambio esperada
# (de menor a mayor volatilidad). Cada COPY genera una capa de imagen
# independiente, así los rebuilds sólo invalidan las capas que cambiaron.
COPY --from=builder --chown=spring:spring /workspace/target/extracted/dependencies/         ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/spring-boot-loader/   ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/application/          ./

USER spring:spring

# Tuning de JVM consciente del contenedor. MaxRAMPercentage hace que el heap
# siga el límite de memoria que asigne Dokploy desde la UI, así no se
# hardcodea ningún valor.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Documenta el puerto dentro del contenedor. Dokploy/Traefik maneja el
# enrutamiento externo.
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/usr/bin/tini", "--", "java", "org.springframework.boot.loader.launch.JarLauncher"]
