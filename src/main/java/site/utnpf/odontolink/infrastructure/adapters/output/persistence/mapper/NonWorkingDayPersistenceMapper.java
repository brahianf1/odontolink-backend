package site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper;

import site.utnpf.odontolink.domain.model.NonWorkingDay;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.NonWorkingDayEntity;

public class NonWorkingDayPersistenceMapper {

    public static NonWorkingDay toDomain(NonWorkingDayEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NonWorkingDay(entity.getDate(), entity.getSource(),
                entity.getName(), entity.getType());
    }
}
