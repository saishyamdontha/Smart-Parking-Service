package com.parking.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableAsync
public class SessionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SessionServiceApplication.class, args);
	}

}
