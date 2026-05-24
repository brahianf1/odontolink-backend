package site.utnpf.odontolink.infrastructure.adapters.output.holidays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

public class ArgentinaDatosHolidayClient {

    private static final Logger log = LoggerFactory.getLogger(ArgentinaDatosHolidayClient.class);

    private final RestClient restClient;

    public ArgentinaDatosHolidayClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<HolidayApiEntry> fetchHolidays(int year) {
        try {
            HolidayApiEntry[] entries = restClient.get()
                    .uri("/v1/feriados/{year}", year)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(HolidayApiEntry[].class);
            if (entries == null) {
                return List.of();
            }
            log.info("Fetched {} holidays for year {} from ArgentinaDatos API", entries.length, year);
            return List.of(entries);
        } catch (Exception e) {
            log.warn("Failed to fetch holidays for year {} from ArgentinaDatos API: {}",
                    year, e.getMessage());
            return List.of();
        }
    }
}
