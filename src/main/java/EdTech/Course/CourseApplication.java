package EdTech.Course;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
@EnableCaching
public class CourseApplication {

	public static void main(String[] args) {
		SpringApplication.run(CourseApplication.class, args);
	}

}
