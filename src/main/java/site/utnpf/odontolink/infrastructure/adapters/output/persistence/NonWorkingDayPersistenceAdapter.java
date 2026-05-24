package site.utnpf.odontolink.infrastructure.adapters.output.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import site.utnpf.odontolink.domain.model.NonWorkingDay;
import site.utnpf.odontolink.domain.model.NonWorkingDaySource;
import site.utnpf.odontolink.domain.repository.NonWorkingDayRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.holidays.ArgentinaDatosHolidayClient;
import site.utnpf.odontolink.infrastructure.adapters.output.holidays.HolidayApiEntry;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.entity.NonWorkingDayEntity;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.jpa_repository.JpaNonWorkingDayRepository;
import site.utnpf.odontolink.infrastructure.adapters.output.persistence.mapper.NonWorkingDayPersistenceMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Transactional(readOnly = true)
public class NonWorkingDayPersistenceAdapter implements NonWorkingDayRepository {

    private static final Logger log = LoggerFactory.getLogger(NonWorkingDayPersistenceAdapter.class);

    private final JpaNonWorkingDayRepository jpaRepository;
    private final ArgentinaDatosHolidayClient holidayClient;
    private final TransactionTemplate requiresNewTx;
    private final long cacheDurationDays;
    private final ReentrantLock refreshLock = new ReentrantLock();

    public NonWorkingDayPersistenceAdapter(
            JpaNonWorkingDayRepository jpaRepository,
            ArgentinaDatosHolidayClient holidayClient,
            PlatformTransactionManager txManager,
            @org.springframework.beans.factory.annotation.Value("${odontolink.holidays.cache-duration-days:30}")
            long holidayCacheDurationDays) {
        this.jpaRepository = jpaRepository;
        this.holidayClient = holidayClient;
        this.cacheDurationDays = holidayCacheDurationDays;
        this.requiresNewTx = new TransactionTemplate(txManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public boolean isNonWorkingDay(LocalDate date) {
        ensureHolidaysLoaded(date.getYear());
        return jpaRepository.existsByDate(date);
    }

    @Override
    public Optional<NonWorkingDay> findByDate(LocalDate date) {
        ensureHolidaysLoaded(date.getYear());
        return jpaRepository.findByDate(date)
                .map(NonWorkingDayPersistenceMapper::toDomain);
    }

    @Override
    public List<NonWorkingDay> findByYear(int year) {
        ensureHolidaysLoaded(year);
        return jpaRepository.findByYear(year).stream()
                .map(NonWorkingDayPersistenceMapper::toDomain)
                .toList();
    }

    private void ensureHolidaysLoaded(int year) {
        Optional<Instant> lastFetched = jpaRepository.findMaxFetchedAtByYearAndSource(
                year, NonWorkingDaySource.NATIONAL_HOLIDAY);

        boolean needsRefresh = lastFetched.isEmpty()
                || lastFetched.get().isBefore(Instant.now().minus(cacheDurationDays, ChronoUnit.DAYS));

        if (!needsRefresh) {
            return;
        }

        if (!refreshLock.tryLock()) {
            // Another thread is already refreshing; if we have stale data, use it
            if (lastFetched.isPresent()) {
                return;
            }
            // No data at all — wait for the other thread to finish
            refreshLock.lock();
            refreshLock.unlock();
            return;
        }

        try {
            refreshFromApi(year, lastFetched.isPresent());
        } finally {
            refreshLock.unlock();
        }
    }

    private void refreshFromApi(int year, boolean hasCachedData) {
        List<HolidayApiEntry> entries = holidayClient.fetchHolidays(year);

        if (entries.isEmpty() && hasCachedData) {
            log.warn("API returned empty for year {}; keeping cached data", year);
            return;
        }

        if (entries.isEmpty()) {
            log.warn("API returned empty for year {} and no cached data exists; " +
                     "non-working day checks will be skipped for this year", year);
            return;
        }

        requiresNewTx.execute(status -> {
            jpaRepository.deleteByYearAndSource(year, NonWorkingDaySource.NATIONAL_HOLIDAY);

            Instant now = Instant.now();
            for (HolidayApiEntry entry : entries) {
                LocalDate date = LocalDate.parse(entry.fecha());
                NonWorkingDayEntity entity = new NonWorkingDayEntity(
                        date,
                        NonWorkingDaySource.NATIONAL_HOLIDAY,
                        entry.nombre(),
                        entry.tipo(),
                        now
                );
                jpaRepository.save(entity);
            }

            log.info("Refreshed {} non-working days (holidays) for year {}", entries.size(), year);
            return null;
        });
    }
}
