package in.ankush.cloudshareapi;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync 

public class CloudshareapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudshareapiApplication.class, args);
	}

}
