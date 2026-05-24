package site.utnpf.odontolink.application.service;

import site.utnpf.odontolink.application.port.in.INonWorkingDayUseCase;
import site.utnpf.odontolink.domain.model.NonWorkingDay;
import site.utnpf.odontolink.domain.repository.NonWorkingDayRepository;

import java.util.List;

public class NonWorkingDayService implements INonWorkingDayUseCase {

    private final NonWorkingDayRepository nonWorkingDayRepository;

    public NonWorkingDayService(NonWorkingDayRepository nonWorkingDayRepository) {
        this.nonWorkingDayRepository = nonWorkingDayRepository;
    }

    @Override
    public List<NonWorkingDay> getByYear(int year) {
        return nonWorkingDayRepository.findByYear(year);
    }
}
