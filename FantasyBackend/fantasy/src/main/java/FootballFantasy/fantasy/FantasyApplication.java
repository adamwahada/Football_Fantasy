package FootballFantasy.fantasy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.TimeZone;

@SpringBootApplication
@EnableMethodSecurity
@EnableScheduling
public class FantasyApplication {

	public static void main(String[] args) {
		SpringApplication.run(FantasyApplication.class, args);
		TimeZone.setDefault(TimeZone.getTimeZone("Africa/Tunis"));

	}
}