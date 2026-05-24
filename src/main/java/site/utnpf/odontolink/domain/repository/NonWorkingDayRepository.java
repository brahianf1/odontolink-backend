package site.utnpf.odontolink.domain.repository;

import site.utnpf.odontolink.domain.model.NonWorkingDay;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NonWorkingDayRepository {

    boolean isNonWorkingDay(LocalDate date);

    Optional<NonWorkingDay> findByDate(LocalDate date);

    List<NonWorkingDay> findByYear(int year);
}
