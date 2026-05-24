package site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity;

import jakarta.persistence.*;
import site.utnpf.odontolink.domain.model.NonWorkingDaySource;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "non_working_days", uniqueConstraints = @UniqueConstraint(columnNames = "date"))
public class NonWorkingDayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NonWorkingDaySource source;

    @Column(nullable = false)
    private String name;

    @Column(length = 30)
    private String type;

    @Column(nullable = false)
    private Instant fetchedAt;

    public NonWorkingDayEntity() {
    }

    public NonWorkingDayEntity(LocalDate date, NonWorkingDaySource source,
                               String name, String type, Instant fetchedAt) {
        this.date = date;
        this.source = source;
        this.name = name;
        this.type = type;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public NonWorkingDaySource getSource() {
        return source;
    }

    public void setSource(NonWorkingDaySource source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}
