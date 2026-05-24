package site.utnpf.odontolink.application.port.in;

import site.utnpf.odontolink.domain.model.NonWorkingDay;

import java.util.List;

public interface INonWorkingDayUseCase {

    List<NonWorkingDay> getByYear(int year);
}
