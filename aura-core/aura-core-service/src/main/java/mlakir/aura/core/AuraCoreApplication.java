package mlakir.aura.core;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableMethodSecurity
@EnableScheduling
public class AuraCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuraCoreApplication.class, args);
    }

}
