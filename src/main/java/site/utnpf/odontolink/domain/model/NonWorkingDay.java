package site.utnpf.odontolink.domain.model;

import java.time.LocalDate;

public class NonWorkingDay {

    private LocalDate date;
    private NonWorkingDaySource source;
    private String name;
    private String type;

    public NonWorkingDay() {
    }

    public NonWorkingDay(LocalDate date, NonWorkingDaySource source, String name, String type) {
        this.date = date;
        this.source = source;
        this.name = name;
        this.type = type;
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
}
