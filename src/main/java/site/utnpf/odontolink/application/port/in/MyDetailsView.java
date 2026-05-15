package site.utnpf.odontolink.application.port.in;

/**
 * Vista inmutable de los datos rol-especificos del usuario autenticado.
 *
 * <p>La capa de aplicacion no acopla a Jackson ni Swagger: este POJO existe
 * para que el caso de uso pueda devolver una estructura union (todos los
 * campos opcionales, llenos solo los aplicables al rol) sin importar el
 * mecanismo de serializacion. El adaptador REST lo mapea a {@code MyDetailsDTO}.
 */
public final class MyDetailsView {

    private final Long userId;
    private final String role;

    // Patient
    private final String healthInsurance;
    private final String bloodType;

    // Practitioner
    private final String studentId;
    private final Integer studyYear;

    // Supervisor
    private final String specialty;
    private final String employeeId;

    public MyDetailsView(Long userId, String role,
                         String healthInsurance, String bloodType,
                         String studentId, Integer studyYear,
                         String specialty, String employeeId) {
        this.userId = userId;
        this.role = role;
        this.healthInsurance = healthInsurance;
        this.bloodType = bloodType;
        this.studentId = studentId;
        this.studyYear = studyYear;
        this.specialty = specialty;
        this.employeeId = employeeId;
    }

    public static MyDetailsView forPatient(Long userId, String role, String healthInsurance, String bloodType) {
        return new MyDetailsView(userId, role, healthInsurance, bloodType, null, null, null, null);
    }

    public static MyDetailsView forPractitioner(Long userId, String role, String studentId, Integer studyYear) {
        return new MyDetailsView(userId, role, null, null, studentId, studyYear, null, null);
    }

    public static MyDetailsView forSupervisor(Long userId, String role, String specialty, String employeeId) {
        return new MyDetailsView(userId, role, null, null, null, null, specialty, employeeId);
    }

    public static MyDetailsView forAdmin(Long userId, String role) {
        return new MyDetailsView(userId, role, null, null, null, null, null, null);
    }

    public Long getUserId() { return userId; }
    public String getRole() { return role; }
    public String getHealthInsurance() { return healthInsurance; }
    public String getBloodType() { return bloodType; }
    public String getStudentId() { return studentId; }
    public Integer getStudyYear() { return studyYear; }
    public String getSpecialty() { return specialty; }
    public String getEmployeeId() { return employeeId; }
}
