package com.hospital.bloodapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class BloodappApplication {

	public static void main(String[] args) {
		SpringApplication.run(BloodappApplication.class, args);
	}

}
