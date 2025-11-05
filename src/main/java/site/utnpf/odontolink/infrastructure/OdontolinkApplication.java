package site.utnpf.odontolink.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class OdontolinkApplication {

    @PostConstruct
    public void init() {
        // Configurar zona horaria de Argentina (UTC-3)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
    }

    public static void main(String[] args) {
        SpringApplication.run(OdontolinkApplication.class, args);
    }

}
