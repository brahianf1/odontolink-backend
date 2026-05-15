package site.utnpf.odontolink.infrastructure.adapters.input.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import site.utnpf.odontolink.application.port.in.IAppointmentUseCase;
import site.utnpf.odontolink.application.port.in.ISearchOfferedTreatmentsUseCase;
import site.utnpf.odontolink.domain.model.Appointment;
import site.utnpf.odontolink.domain.model.Attention;
import site.utnpf.odontolink.domain.model.OfferedTreatment;
import site.utnpf.odontolink.domain.model.OfferedTreatmentSearchCriteria;
import site.utnpf.odontolink.domain.model.PageQuery;
import site.utnpf.odontolink.domain.model.PageResult;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.AppointmentRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.request.CancelAppointmentByPatientRequestDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AppointmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.AttentionResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.ErrorResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.OfferedTreatmentResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.dto.response.PageResponseDTO;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AppointmentRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.AttentionRestMapper;
import site.utnpf.odontolink.infrastructure.adapters.input.rest.mapper.OfferedTreatmentRestMapper;
import site.utnpf.odontolink.infrastructure.security.AuthenticationFacade;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para las operaciones del paciente.
 * Adaptador de entrada (Input Adapter) en Arquitectura Hexagonal.
 *
 * Endpoints implementados:
 * - GET    /api/patient/offered-treatments                      - Ver catálogo de tratamientos
 * - GET    /api/patient/offered-treatments/{id}/availability    - Ver slots disponibles (inventario dinámico)
 * - POST   /api/patient/appointments                            - Reservar turno (CU-008)
 * - GET    /api/patient/appointments/upcoming                   - Ver mis turnos agendados
 *
 * Todos los endpoints están protegidos con @PreAuthorize("hasRole('PATIENT')").
 *
 * @author OdontoLink Team
 */
@RestController
@RequestMapping("/api/patient")
@PreAuthorize("hasRole('PATIENT')")
@Tag(name = "Pacientes", description = "Operaciones disponibles para pacientes: consultar tratamientos, reservar citas y gestionar sus turnos")
@SecurityRequirement(name = "Bearer Authentication")
public class PatientController {

    private final IAppointmentUseCase appointmentUseCase;
    private final ISearchOfferedTreatmentsUseCase searchOfferedTreatmentsUseCase;
    private final AuthenticationFacade authenticationFacade;

    public PatientController(IAppointmentUseCase appointmentUseCase,
                            ISearchOfferedTreatmentsUseCase searchOfferedTreatmentsUseCase,
                            AuthenticationFacade authenticationFacade) {
        this.appointmentUseCase = appointmentUseCase;
        this.searchOfferedTreatmentsUseCase = searchOfferedTreatmentsUseCase;
        this.authenticationFacade = authenticationFacade;
    }

    /**
     * Catálogo público con motor de búsqueda dinámica (RF09) + paginación (UX/Performance).
     *
     * Los filtros son OPCIONALES y combinables vía AND:
     *  - keyword: busca en nombre/descripción del tratamiento y en nombre/apellido del practicante (case-insensitive).
     *  - specialty: área odontológica exacta del tratamiento (case-insensitive).
     *  - availability: día de la semana (DayOfWeek: MONDAY..SUNDAY) sobre el que la oferta publica disponibilidad.
     *
     * Paginación:
     *  - page: 0-based, default 0.
     *  - size: default {@link PageQuery#DEFAULT_PAGE_SIZE}, máximo {@link PageQuery#MAX_PAGE_SIZE}.
     *  - sortBy: alias permitido (treatmentName | specialty | duration | offerStartDate | offerEndDate | id).
     *  - sortDirection: ASC (default) | DESC.
     *
     * Sólo se retornan ofertas {@code active=true}: las bajas lógicas de RF16
     * no se exponen al paciente bajo ninguna combinación de filtros.
     *
     * GET /api/patient/offered-treatments
     */
    @Operation(
            summary = "Buscar catálogo público de tratamientos (RF09)",
            description = "Motor de búsqueda dinámica y paginada del catálogo público de ofertas de " +
                    "tratamientos.\n\n" +
                    "**Filtros opcionales (combinables con AND lógico):**\n" +
                    "- `keyword`: texto libre que se busca, case-insensitive, en el nombre/descripción del " +
                    "tratamiento maestro y en el nombre/apellido del practicante. Útil para una barra de " +
                    "búsqueda única en la UI.\n" +
                    "- `specialty`: área odontológica exacta del tratamiento (ej. `ORTODONCIA`, `ENDODONCIA`). " +
                    "Pensado para filtros por categoría.\n" +
                    "- `availability`: día de la semana (`DayOfWeek` en inglés: `MONDAY`..`SUNDAY`) sobre el " +
                    "que la oferta publica al menos un slot de disponibilidad. NO valida que aún queden " +
                    "horarios libres ese día — para eso usar `GET /offered-treatments/{id}/availability`.\n\n" +
                    "**Paginación y ordenamiento:**\n" +
                    "- `page` es 0-based (la primera página es `0`). Valores negativos se sanitizan a `0`.\n" +
                    "- `size` por defecto `20`, máximo `100` (excederlo responde `422`).\n" +
                    "- `sortBy` acepta sólo un allowlist (`treatmentName` | `specialty` | `duration` | " +
                    "`offerStartDate` | `offerEndDate` | `id`); cualquier otro valor se ignora.\n" +
                    "- `sortDirection` ∈ {`ASC` (default), `DESC`}; otros valores se sanitizan a `ASC`.\n\n" +
                    "**Política de visibilidad**: sólo se retornan ofertas `active=true`. Las bajas lógicas " +
                    "de RF16 se ocultan al paciente bajo CUALQUIER combinación de filtros."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Página de ofertas que satisfacen los criterios (posiblemente vacía).",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Catálogo paginado",
                                    value = """
                                            {
                                              "content": [
                                                {
                                                  "id": 1,
                                                  "practitionerId": 1,
                                                  "practitionerName": "Maria Gomez",
                                                  "treatment": {
                                                    "id": 1,
                                                    "name": "Limpieza completa",
                                                    "description": "Eliminación de placa y sarro total",
                                                    "area": "ORTODONCIA"
                                                  },
                                                  "requirements": "Traer cepillo dental propio",
                                                  "durationInMinutes": 60,
                                                  "availabilitySlots": [
                                                    {
                                                      "dayOfWeek": "MONDAY",
                                                      "startTime": "08:00:00",
                                                      "endTime": "12:00:00"
                                                    }
                                                  ],
                                                  "offerStartDate": "2025-01-15",
                                                  "offerEndDate": "2025-06-30",
                                                  "maxCompletedAttentions": 10,
                                                  "currentCompletedAttentions": 3,
                                                  "currentActiveAttentions": 2,
                                                  "currentCancelledAttentions": 1,
                                                  "availabilityBlocked": false,
                                                  "status": "ACTIVE"
                                                }
                                              ],
                                              "page": 0,
                                              "size": 20,
                                              "totalElements": 1,
                                              "totalPages": 1,
                                              "hasNext": false,
                                              "hasPrevious": false
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetros con tipo inválido (por ejemplo `availability=LUN` no es un " +
                            "`DayOfWeek` válido en inglés, o `page` no es un entero).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee el rol PATIENT.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Parámetros de paginación fuera de rango (por ejemplo `size > 100`).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Tamaño de página excedido",
                                    value = """
                                            {
                                              "timestamp": "2025-11-10T10:00:00",
                                              "status": 422,
                                              "error": "Business Rule Violation",
                                              "message": "El tamaño de página no puede exceder 100 elementos.",
                                              "path": "/api/patient/offered-treatments"
                                            }
                                            """
                            ))
            )
    })
    @GetMapping("/offered-treatments")
    public ResponseEntity<PageResponseDTO<OfferedTreatmentResponseDTO>> searchAvailableTreatments(
            @Parameter(description = "Texto libre buscado, case-insensitive, en nombre/descripción del " +
                    "tratamiento y en nombre/apellido del practicante.",
                    example = "limpieza")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Área/Especialidad odontológica exacta del tratamiento.",
                    example = "ORTODONCIA")
            @RequestParam(required = false) String specialty,
            @Parameter(description = "Día de la semana sobre el que la oferta publica al menos un slot. " +
                    "Valor en inglés del enum `java.time.DayOfWeek`.",
                    example = "MONDAY",
                    schema = @Schema(allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
                            "FRIDAY", "SATURDAY", "SUNDAY"}))
            @RequestParam(required = false) DayOfWeek availability,
            @Parameter(description = "Número de página solicitada, 0-based. Valores negativos se " +
                    "sanitizan a `0`.",
                    example = "0")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @Parameter(description = "Cantidad de elementos por página. Default `20`, máximo `100`. " +
                    "Excederlo responde `422`.",
                    example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @Parameter(description = "Campo de ordenamiento. Sólo se aceptan valores del allowlist; " +
                    "cualquier otro valor se ignora silenciosamente y se devuelve el orden por defecto.",
                    example = "treatmentName",
                    schema = @Schema(allowableValues = {"treatmentName", "specialty", "duration",
                            "offerStartDate", "offerEndDate", "id"}))
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Dirección del ordenamiento. Valores fuera del enum se sanitizan a `ASC`.",
                    example = "ASC",
                    schema = @Schema(allowableValues = {"ASC", "DESC"}))
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection) {

        OfferedTreatmentSearchCriteria criteria =
                new OfferedTreatmentSearchCriteria(keyword, specialty, availability);
        PageQuery pageQuery = PageQuery.of(page, size, sortBy, sortDirection);

        PageResult<OfferedTreatment> result =
                searchOfferedTreatmentsUseCase.search(criteria, pageQuery);

        PageResponseDTO<OfferedTreatmentResponseDTO> response =
                PageResponseDTO.of(result, OfferedTreatmentRestMapper::toResponse);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Obtener slots de tiempo disponibles para una oferta en una fecha",
            description = "Calcula y devuelve, **en tiempo real**, los horarios libres en los que el paciente " +
                    "puede reservar un turno para la oferta indicada y la fecha solicitada (modelo de " +
                    "inventario dinámico).\n\n" +
                    "**Algoritmo:**\n" +
                    "1. Se generan los slots teóricos a partir de los `AvailabilitySlot` publicados por la " +
                    "oferta para el `dayOfWeek` correspondiente a `date`, partiendo el bloque por la duración " +
                    "del servicio (`durationInMinutes`).\n" +
                    "2. Se descartan los slots cuyo rango `[start, start+duration)` se solape con un " +
                    "`Appointment` ya reservado y no cancelado del practicante.\n" +
                    "3. Se devuelve sólo el remanente — es decir, las opciones que el frontend puede " +
                    "ofrecer al paciente y que efectivamente sobrevivirían a `POST /appointments` por la " +
                    "regla de Double-booking.\n\n" +
                    "**Importante para el frontend**: si la respuesta es `[]`, NO hay horarios para esa " +
                    "fecha (sea porque la oferta no atiende ese día, porque está totalmente reservada, o " +
                    "porque `date` cae fuera de `offerStartDate`/`offerEndDate`). El frontend debería " +
                    "mostrar un estado vacío explícito en lugar de un spinner perpetuo."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de instantes disponibles para reservar, ordenada cronológicamente. " +
                            "Cada elemento es un `LocalDateTime` ISO-8601 sin zona horaria que puede " +
                            "reenviarse tal cual como `appointmentTime` en `POST /api/patient/appointments`. " +
                            "Puede ser una lista vacía.",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(type = "string",
                                    format = "date-time",
                                    example = "2025-12-08T08:00:00")),
                            examples = @ExampleObject(
                                    name = "Slots de la fecha",
                                    value = """
                                            [
                                              "2025-12-08T08:00:00",
                                              "2025-12-08T09:00:00",
                                              "2025-12-08T10:00:00",
                                              "2025-12-08T11:00:00"
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "`date` no respeta el formato ISO `yyyy-MM-dd` (ej. `15/12/2025`).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee el rol PATIENT.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "La oferta de tratamiento indicada por `offeredTreatmentId` no existe.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Oferta no encontrada",
                                    value = """
                                            {
                                              "timestamp": "2025-11-10T10:00:00",
                                              "status": 404,
                                              "error": "Resource Not Found",
                                              "message": "OfferedTreatment no encontrado con id: 999",
                                              "path": "/api/patient/offered-treatments/999/availability"
                                            }
                                            """
                            ))
            )
    })
    @GetMapping("/offered-treatments/{offeredTreatmentId}/availability")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(
            @Parameter(description = "ID de la oferta del catálogo (OfferedTreatment) sobre la que se " +
                    "consulta disponibilidad.",
                    example = "1",
                    required = true)
            @PathVariable Long offeredTreatmentId,
            @Parameter(description = "Fecha del día a consultar, en formato ISO `yyyy-MM-dd`. " +
                    "Se interpreta en la zona horaria del servidor; el frontend NO debe enviar zona " +
                    "horaria ni hora.",
                    example = "2025-12-08",
                    required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // Delegar al caso de uso para calcular el inventario dinámico
        List<LocalDateTime> availableSlots = appointmentUseCase.getAvailableSlots(offeredTreatmentId, date);

        return ResponseEntity.ok(availableSlots);
    }

    @Operation(
            summary = "Reservar turno (CU-008)",
            description = "Reserva un turno aplicando el modelo *intent-driven* completo.\n\n" +
                    "**Comportamiento de agrupación (Atención):**\n" +
                    "- Si NO existe una Atención `IN_PROGRESS` para el trío " +
                    "paciente/practicante/tratamiento, se crea atómicamente una nueva Atención + " +
                    "Appointment. El motivo del turno se etiqueta como `Primer turno - Inicio de tratamiento`.\n" +
                    "- Si ya existe una Atención `IN_PROGRESS`, el nuevo turno se agrupa dentro de ella " +
                    "y se etiqueta como `Turno adicional del caso`.\n\n" +
                    "**Reglas de negocio que pueden bloquear la reserva (causan `422`):**\n" +
                    "1. **Horario fuera de disponibilidad**: el `appointmentTime` cae fuera de los " +
                    "`AvailabilitySlot` publicados por la oferta para ese día de la semana.\n" +
                    "2. **Double-booking del paciente**: el paciente ya tiene otro turno no cancelado a la " +
                    "misma hora.\n" +
                    "3. **Double-booking del practicante (colisión por rango)**: el rango " +
                    "`[appointmentTime, appointmentTime + durationInMinutes)` se solapa con otro turno " +
                    "ya reservado del practicante.\n" +
                    "4. **Anti-Acaparamiento**: el paciente ya tiene el límite dinámico de turnos " +
                    "`SCHEDULED` activos dentro de esta Atención (límite leído de `InstitutionalSettings`, " +
                    "configurable por el administrador).\n\n" +
                    "**Efectos colaterales transaccionales:** al confirmarse la reserva, si no existe " +
                    "una `ChatSession` entre paciente y practicante, se crea automáticamente (RF27).\n\n" +
                    "**Seguridad / Anti-IDOR**: el `patientId` se infiere del JWT — el frontend NO debe " +
                    "enviarlo en el body."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Turno reservado exitosamente. Se devuelve la Atención (nueva o " +
                            "preexistente) con el nuevo Appointment ya incluido en su lista `appointments`.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AttentionResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Reserva aceptada",
                                    value = """
                                            {
                                              "id": 1,
                                              "status": "IN_PROGRESS",
                                              "startDate": "2025-11-02",
                                              "patientId": 1,
                                              "patientName": "Lucas Malla",
                                              "practitionerId": 1,
                                              "practitionerName": "Maria Gomez",
                                              "treatmentId": 1,
                                              "treatmentName": "Limpieza completa",
                                              "appointments": [
                                                {
                                                  "id": 1,
                                                  "appointmentTime": "2025-12-08T11:00:00",
                                                  "motive": "Primer turno - Inicio de tratamiento",
                                                  "status": "SCHEDULED",
                                                  "durationInMinutes": 60,
                                                  "treatmentId": 1,
                                                  "treatmentName": "Limpieza completa",
                                                  "patientId": 1,
                                                  "patientName": "Lucas Malla",
                                                  "practitionerId": 1,
                                                  "practitionerName": "Maria Gomez",
                                                  "attentionId": 1
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bean Validation falló: `offeredTreatmentId` nulo, `appointmentTime` " +
                            "nulo o pasado (no `@Future`), JSON malformado, etc.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Fecha en el pasado",
                                    value = """
                                            {
                                              "timestamp": "2025-11-10T10:00:00",
                                              "status": 400,
                                              "error": "Validation Error",
                                              "message": "Los datos proporcionados no son válidos",
                                              "path": "/api/patient/appointments",
                                              "details": [
                                                "appointmentTime: La fecha del turno debe ser futura"
                                              ]
                                            }
                                            """
                            ))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee el rol PATIENT.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "El `offeredTreatmentId` no existe en el catálogo o el paciente " +
                            "autenticado no se encuentra.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Oferta inexistente",
                                    value = """
                                            {
                                              "timestamp": "2025-11-10T10:00:00",
                                              "status": 404,
                                              "error": "Resource Not Found",
                                              "message": "OfferedTreatment no encontrado con id: 999",
                                              "path": "/api/patient/appointments"
                                            }
                                            """
                            ))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Violación de regla de negocio: horario fuera de disponibilidad, " +
                            "Double-booking (colisión con otro turno) o Anti-Acaparamiento (límite de " +
                            "turnos activos por atención alcanzado).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Double-booking — horario solapado con el practicante",
                                            summary = "El practicante ya tiene un turno que pisa el rango",
                                            value = """
                                                    {
                                                      "timestamp": "2025-11-10T10:00:00",
                                                      "status": 422,
                                                      "error": "Business Rule Violation",
                                                      "message": "El practicante ya tiene un turno agendado que se solapa con el horario solicitado. Por favor, seleccione otro horario disponible.",
                                                      "path": "/api/patient/appointments"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Double-booking — el paciente ya tiene un turno a esa hora",
                                            summary = "El paciente intenta reservar dos turnos a la vez",
                                            value = """
                                                    {
                                                      "timestamp": "2025-11-10T10:00:00",
                                                      "status": 422,
                                                      "error": "Business Rule Violation",
                                                      "message": "Ya tiene un turno agendado para esta fecha y hora. No puede reservar dos turnos al mismo tiempo.",
                                                      "path": "/api/patient/appointments"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Anti-Acaparamiento — límite de turnos activos alcanzado",
                                            summary = "El paciente ya consumió todos sus turnos SCHEDULED " +
                                                    "permitidos en esta atención",
                                            value = """
                                                    {
                                                      "timestamp": "2025-11-10T10:00:00",
                                                      "status": 422,
                                                      "error": "Business Rule Violation",
                                                      "message": "No puede reservar otro turno para este caso clínico mientras tenga 1 turno(s) agendado(s) pendiente(s). Espere a que se complete o cancele para reservar uno nuevo.",
                                                      "path": "/api/patient/appointments"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Horario fuera de disponibilidad publicada",
                                            summary = "El `appointmentTime` no cae en ningún " +
                                                    "AvailabilitySlot de la oferta",
                                            value = """
                                                    {
                                                      "timestamp": "2025-11-10T10:00:00",
                                                      "status": 422,
                                                      "error": "Business Rule Violation",
                                                      "message": "El horario seleccionado no está disponible. Por favor, elija un horario dentro de la disponibilidad publicada del practicante.",
                                                      "path": "/api/patient/appointments"
                                                    }
                                                    """
                                    )
                            })
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos del turno a reservar. El `appointmentTime` debería elegirse de la " +
                    "lista devuelta por `GET /offered-treatments/{id}/availability` para minimizar " +
                    "errores `422` por horario inválido.",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AppointmentRequestDTO.class),
                    examples = @ExampleObject(
                            name = "Reserva de turno",
                            value = """
                                    {
                                      "offeredTreatmentId": 1,
                                      "appointmentTime": "2025-12-08T11:00:00"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/appointments")
    public ResponseEntity<AttentionResponseDTO> scheduleAppointment(
            @Valid @RequestBody AppointmentRequestDTO request) {

        // Obtener el ID del paciente autenticado
        Long patientId = authenticationFacade.getAuthenticatedPatientId();

        // Delegar al caso de uso (servicio de aplicación).
        // El caso de uso decide si crea una Atención nueva o agrupa el turno
        // dentro de una IN_PROGRESS existente, y aplica la regla
        // anti-acaparamiento usando el límite dinámico de InstitutionalSettings.
        Attention attention = appointmentUseCase.bookAppointment(
                patientId,
                request.getOfferedTreatmentId(),
                request.getAppointmentTime()
        );

        // Convertir a DTO de respuesta
        AttentionResponseDTO response = AttentionRestMapper.toResponse(attention);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Cancelar turno por iniciativa del paciente",
            description = "Cancela un turno en estado `SCHEDULED` por decisión del paciente.\n\n" +
                    "**Diferencia clave frente a la cancelación del practicante:** para el paciente el " +
                    "campo `cancellationReason` es **OPCIONAL**. El paciente NO está obligado a " +
                    "justificarse — puede enviar el body sin `reason`, con `reason` vacío, o incluso " +
                    "omitir el body por completo. Si lo informa, el texto se persiste para enriquecer " +
                    "el funnel de deserción.\n\n" +
                    "**Reglas:**\n" +
                    "- El turno debe existir y estar en estado `SCHEDULED` (cancelar uno `COMPLETED`, " +
                    "`NO_SHOW` o ya `CANCELLED` responde `422`).\n" +
                    "- El paciente autenticado debe ser el titular del turno (defensa de ownership " +
                    "contra IDOR: forzar el `appointmentId` de otro paciente responde `403`).\n\n" +
                    "**Efecto colateral (funnel tracking):** tras cancelar, si la Atención padre queda " +
                    "sin trabajo clínico realizado ni próximos turnos `SCHEDULED`, se cierra " +
                    "automáticamente como `CANCELLED`."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Turno cancelado correctamente. Se devuelve el Appointment actualizado " +
                            "con `status=CANCELLED` y, si correspondió, el `cancellationReason` persistido.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AppointmentResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Turno cancelado con motivo",
                                    value = """
                                            {
                                              "id": 45,
                                              "appointmentTime": "2025-12-08T11:00:00",
                                              "motive": "Primer turno - Inicio de tratamiento",
                                              "status": "CANCELLED",
                                              "durationInMinutes": 60,
                                              "cancellationReason": "Me surgió un imprevisto y no puedo asistir.",
                                              "treatmentId": 1,
                                              "treatmentName": "Limpieza completa",
                                              "patientId": 1,
                                              "patientName": "Lucas Malla",
                                              "practitionerId": 1,
                                              "practitionerName": "Maria Gomez",
                                              "attentionId": 1
                                            }
                                            """
                            ))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bean Validation falló — el motivo informado supera los 1000 caracteres.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El paciente autenticado NO es el titular de la Atención del turno " +
                            "(intento de cancelar el turno de otro paciente) o el usuario no posee el " +
                            "rol PATIENT.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Turno de otro paciente",
                                    value = """
                                            {
                                              "timestamp": "2025-11-10T10:00:00",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "Solo el paciente titular de la atención puede operar sobre este turno.",
                                              "path": "/api/patient/appointments/45/cancel"
                                            }
                                            """
                            ))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe ningún Appointment con el `appointmentId` indicado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "El turno no está en estado `SCHEDULED` (ya está completado, marcado " +
                            "como ausente o cancelado previamente).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Turno no cancelable",
                                    value = """
                                            {
                                              "timestamp": "2025-11-10T10:00:00",
                                              "status": 422,
                                              "error": "Business Rule Violation",
                                              "message": "Solo se puede cancelar un turno en estado SCHEDULED.",
                                              "path": "/api/patient/appointments/45/cancel"
                                            }
                                            """
                            ))
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Body OPCIONAL. Puede omitirse por completo, enviarse vacío `{}` o incluir un " +
                    "`reason` con el texto del paciente.",
            required = false,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CancelAppointmentByPatientRequestDTO.class),
                    examples = {
                            @ExampleObject(
                                    name = "Cancelación con motivo",
                                    summary = "El paciente decide justificarse",
                                    value = """
                                            {
                                              "reason": "Me surgió un imprevisto y no puedo asistir."
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Cancelación sin motivo",
                                    summary = "Body vacío — el motivo es opcional para el paciente",
                                    value = "{}"
                            )
                    }
            )
    )
    @PostMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(
            @Parameter(description = "ID del turno (`Appointment`) a cancelar.",
                    example = "45",
                    required = true)
            @PathVariable Long appointmentId,
            @Valid @RequestBody(required = false) CancelAppointmentByPatientRequestDTO request) {

        site.utnpf.odontolink.domain.model.User patientUser = authenticationFacade.getAuthenticatedUser();
        // El motivo es opcional: si el body viene vacío o sin reason, se pasa null
        // y el dominio normaliza la ausencia de texto sin error.
        String reason = (request != null) ? request.getReason() : null;

        Appointment cancelled = appointmentUseCase.cancelAppointmentByPatient(
                appointmentId,
                reason,
                patientUser
        );

        return ResponseEntity.ok(AppointmentRestMapper.toResponse(cancelled));
    }

    @Operation(
            summary = "Listar mis turnos agendados (Mis Turnos)",
            description = "Devuelve todos los turnos en estado `SCHEDULED` del paciente autenticado, " +
                    "pensado para alimentar la vista \"Mis Turnos\" del panel del paciente.\n\n" +
                    "**No se incluyen** los turnos `COMPLETED`, `NO_SHOW` ni `CANCELLED` — para el " +
                    "histórico clínico se debe usar el módulo de Atenciones.\n\n" +
                    "**Seguridad / Anti-IDOR**: el `patientId` se infiere del JWT — no recibe " +
                    "parámetros del cliente."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de turnos `SCHEDULED` del paciente (posiblemente vacía).",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = AppointmentResponseDTO.class)),
                            examples = @ExampleObject(
                                    name = "Mis turnos",
                                    value = """
                                            [
                                              {
                                                "id": 45,
                                                "appointmentTime": "2025-12-08T11:00:00",
                                                "motive": "Primer turno - Inicio de tratamiento",
                                                "status": "SCHEDULED",
                                                "durationInMinutes": 60,
                                                "cancellationReason": null,
                                                "treatmentId": 1,
                                                "treatmentName": "Limpieza completa",
                                                "patientId": 1,
                                                "patientName": "Lucas Malla",
                                                "practitionerId": 1,
                                                "practitionerName": "Maria Gomez",
                                                "attentionId": 23
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente o inválido.",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "El usuario autenticado no posee el rol PATIENT.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "El paciente autenticado no existe en la base (inconsistencia entre " +
                            "el JWT y el estado actual de la BD).",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDTO.class))
            )
    })
    @GetMapping("/appointments/upcoming")
    public ResponseEntity<List<AppointmentResponseDTO>> getMyUpcomingAppointments() {

        Long patientId = authenticationFacade.getAuthenticatedPatientId();

        List<Appointment> appointments = appointmentUseCase.getUpcomingAppointmentsForPatient(patientId);

        List<AppointmentResponseDTO> response = appointments.stream()
                .map(AppointmentRestMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}

