package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.AvailabilitySlotEntity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface JpaAvailabilitySlotRepository extends JpaRepository<AvailabilitySlotEntity, Long> {

    List<AvailabilitySlotEntity> findByOfferedTreatment_Id(Long offeredTreatmentId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM AvailabilitySlotEntity a " +
           "WHERE a.offeredTreatment.id = :offeredTreatmentId " +
           "AND a.dayOfWeek = :dayOfWeek " +
           "AND :time >= a.startTime AND :time < a.endTime")
    boolean isTimeWithinAvailability(
            @Param("offeredTreatmentId") Long offeredTreatmentId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("time") LocalTime time
    );
}
