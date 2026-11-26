package com.example.arcturus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArcturusApplication {

	public static void main(String[] args) {
		SpringApplication.run(ArcturusApplication.class, args);
	}

}
