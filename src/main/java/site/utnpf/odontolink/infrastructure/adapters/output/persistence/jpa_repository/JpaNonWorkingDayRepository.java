package site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.utnpf.odontolink.domain.model.NonWorkingDaySource;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.NonWorkingDayEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaNonWorkingDayRepository extends JpaRepository<NonWorkingDayEntity, Long> {

    boolean existsByDate(LocalDate date);

    Optional<NonWorkingDayEntity> findByDate(LocalDate date);

    @Query("SELECT h FROM NonWorkingDayEntity h WHERE YEAR(h.date) = :year ORDER BY h.date")
    List<NonWorkingDayEntity> findByYear(@Param("year") int year);

    @Query("SELECT MAX(h.fetchedAt) FROM NonWorkingDayEntity h " +
           "WHERE YEAR(h.date) = :year AND h.source = :source")
    Optional<Instant> findMaxFetchedAtByYearAndSource(@Param("year") int year,
                                                      @Param("source") NonWorkingDaySource source);

    @Modifying
    @Query("DELETE FROM NonWorkingDayEntity h WHERE YEAR(h.date) = :year AND h.source = :source")
    void deleteByYearAndSource(@Param("year") int year, @Param("source") NonWorkingDaySource source);
}
