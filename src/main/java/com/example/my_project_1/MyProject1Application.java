package com.example.my_project_1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@EnableRetry
@EnableAsync
@SpringBootApplication
public class MyProject1Application {

	public static void main(String[] args) {
		SpringApplication.run(MyProject1Application.class, args);
	}

}
